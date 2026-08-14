package com.zhouyp.justdid.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.zhouyp.justdid.data.local.db.dao.DailyReportIndexDao
import com.zhouyp.justdid.data.local.db.entity.MobileDailyReportIndexEntity
import com.zhouyp.justdid.data.remote.dto.PushResponse
import com.zhouyp.justdid.domain.BatchIdGenerator
import com.zhouyp.justdid.domain.model.PushResult
import com.zhouyp.justdid.domain.model.UpdatedIndexEntry
import com.zhouyp.justdid.domain.repository.PushRepository
import com.zhouyp.justdid.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val batchIdGenerator: BatchIdGenerator,
    private val indexDao: DailyReportIndexDao,
    @ApplicationContext private val context: Context
) : PushRepository {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "PushRepository"
        private const val STAGING_DIR = "staging"
        private const val MESSAGE_SUCCESS = "同步成功"
        private val DISCARDABLE_MESSAGES = setOf("批数据解析失败", "批数据解压后超过 5MB 上限")
    }

    private val filesDir: File
        get() = context.filesDir
    private val stagingDir: File
        get() = File(filesDir, STAGING_DIR)

    override suspend fun push(): PushResult = withContext(Dispatchers.IO) {
        val batchId = findExistingBatchId() ?: createBatchFromStaging()
            ?: return@withContext PushResult.NoData
        pushBatch(batchId)
    }

    override suspend fun discardBatch(batchId: String) {
        withContext(Dispatchers.IO) {
            File(filesDir, batchId).deleteRecursively()
            Log.d(TAG, "丢弃批文件夹: $batchId")
        }
    }

    private fun findExistingBatchId(): String? {
        val batchId = filesDir.listFiles()
            ?.filter { it.isDirectory && batchIdGenerator.isBatchId(it.name) }
            ?.firstOrNull()
            ?.name
        if (batchId != null) {
            Log.d(TAG, "发现已有批文件夹: $batchId")
        }
        return batchId
    }

    private fun createBatchFromStaging(): String? {
        val hasContent = stagingDir.listFiles().orEmpty().any { it.isFile }
        if (!hasContent) {
            Log.d(TAG, "staging 无内容，不创建批")
            return null
        }

        val batchId = batchIdGenerator.generate()
        val batchDir = File(filesDir, batchId)
        return if (stagingDir.renameTo(batchDir)) {
            Log.d(TAG, "staging 重命名为批文件夹: $batchId")
            batchId
        } else {
            Log.e(TAG, "staging 重命名失败")
            null
        }
    }

    private suspend fun pushBatch(batchId: String): PushResult {
        val batchDir = File(filesDir, batchId)
        val zipFile = zipBatch(batchDir) ?: return PushResult.Error("打包失败，请稍后重试")
        return try {
            val rootUrl = settingsRepository.getMasterRootUrl()
                ?: return PushResult.Error("未连接到电脑端")
            val request = Request.Builder()
                .url("$rootUrl/sync/submit")
                .header("X-Batch-ID", batchId)
                .post(zipFile.asRequestBody("application/octet-stream".toMediaType()))
                .build()
            Log.d(TAG, "推送请求: ${request.url}, X-Batch-ID=$batchId")
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d(TAG, "推送响应: code=${response.code}, body=$body")
                classifyResponse(batchId, body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "推送异常: ${e.javaClass.simpleName}: ${e.message}", e)
            PushResult.Error("推送失败，请稍后重试")
        } finally {
            zipFile.delete()
        }
    }

    private fun zipBatch(batchDir: File): File? {
        return try {
            val zipFile = File(context.cacheDir, "${batchDir.name}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                batchDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val entryPath = "${batchDir.name}/${file.relativeTo(batchDir).path.replace('\\', '/')}"
                    zos.putNextEntry(ZipEntry(entryPath))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            Log.d(TAG, "打包完成: ${zipFile.path}")
            zipFile
        } catch (e: Exception) {
            Log.e(TAG, "打包异常: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    private suspend fun classifyResponse(batchId: String, body: String?): PushResult {
        val response = try {
            gson.fromJson(body, PushResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "响应解析异常", e)
            return PushResult.Error("推送失败，请稍后重试")
        }
        val message = response.message ?: return PushResult.Error("推送失败，请稍后重试")
        return when (message) {
            MESSAGE_SUCCESS -> {
                File(filesDir, batchId).deleteRecursively()
                val updatedIndex = response.updatedIndex.orEmpty()
                indexDao.upsertAll(updatedIndex.map {
                    MobileDailyReportIndexEntity(
                        year = it.year,
                        month = it.month,
                        day = it.day,
                        path = it.path.orEmpty().removePrefix("data/"),
                        fileSize = it.fileSize,
                        status = 0
                    )
                })
                Log.d(TAG, "推送成功，批文件夹已删除并初始化索引: $batchId")
                PushResult.Success(updatedIndex.map {
                    UpdatedIndexEntry(
                        year = it.year,
                        month = it.month,
                        day = it.day,
                        path = it.path.orEmpty().removePrefix("data/"),
                        fileSize = it.fileSize
                    )
                })
            }
            in DISCARDABLE_MESSAGES -> PushResult.DiscardableError(batchId, message)
            else -> PushResult.Error(message)
        }
    }
}
