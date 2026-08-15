package com.zhouyp.justdid.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.zhouyp.justdid.data.local.db.dao.DailyReportIndexDao
import com.zhouyp.justdid.data.local.db.entity.MobileDailyReportIndexEntity
import com.zhouyp.justdid.data.remote.dto.FetchIndexResponse
import com.zhouyp.justdid.domain.model.FetchIndexResult
import com.zhouyp.justdid.domain.model.FetchResult
import com.zhouyp.justdid.domain.repository.DailyReportRepository
import com.zhouyp.justdid.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReportRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val indexDao: DailyReportIndexDao,
    @ApplicationContext private val context: Context
) : DailyReportRepository {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "DailyReportRepo"
        private const val ERROR_MESSAGE = "拉取失败，请稍后重试"
        private val DATE_REQUEST_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        private val ENTRY_NAME_REGEX = Regex("""^\d{4}/\d{2}/\d{2}\.txt$""")
    }

    override suspend fun fetch(dates: List<LocalDate>): FetchResult = withContext(Dispatchers.IO) {
        if (dates.isEmpty()) return@withContext FetchResult.Error(ERROR_MESSAGE)
        val rootUrl = settingsRepository.getMasterRootUrl()
            ?: return@withContext FetchResult.Error(ERROR_MESSAGE)

        val bodyJson = gson.toJson(mapOf("dates" to dates.map { it.format(DATE_REQUEST_FORMAT) }))
        val request = Request.Builder()
            .url("$rootUrl/sync/fetch")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()
        Log.d(TAG, "拉取请求: ${request.url}, dates=${dates.size}")

        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> {
                        Log.d(TAG, "拉取响应 404 无文件")
                        updateIndexAfterFetch(dates, emptySet())
                        FetchResult.NotFound
                    }
                    response.isSuccessful -> {
                        val body = response.body
                        if (body == null) {
                            FetchResult.Error(ERROR_MESSAGE)
                        } else {
                            // 电脑端 zip 为流式打包（本地头 size=0），ZipInputStream 会抛 invalid entry size，
                            // 先落盘再用 ZipFile 按中央目录解析
                            val tmpZip = File(context.cacheDir, "fetch-${System.currentTimeMillis()}.zip")
                            try {
                                tmpZip.outputStream().use { out ->
                                    body.byteStream().use { it.copyTo(out) }
                                }
                                val extractedDates = extractZip(tmpZip)
                                if (extractedDates == null) {
                                    FetchResult.Error(ERROR_MESSAGE)
                                } else {
                                    Log.d(TAG, "解压条目数: ${extractedDates.size}")
                                    updateIndexAfterFetch(dates, extractedDates)
                                    FetchResult.Success
                                }
                            } finally {
                                tmpZip.delete()
                            }
                        }
                    }
                    else -> {
                        Log.w(TAG, "拉取响应异常: code=${response.code}")
                        FetchResult.Error(ERROR_MESSAGE)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "拉取异常: ${e.javaClass.simpleName}: ${e.message}", e)
            FetchResult.Error(ERROR_MESSAGE)
        }
    }

    override suspend fun fetchIndex(dates: List<LocalDate>): FetchIndexResult = withContext(Dispatchers.IO) {
        if (dates.isEmpty()) return@withContext FetchIndexResult.Error(ERROR_MESSAGE)
        val rootUrl = settingsRepository.getMasterRootUrl()
            ?: return@withContext FetchIndexResult.Error(ERROR_MESSAGE)

        val bodyJson = gson.toJson(mapOf("dates" to dates.map { it.format(DATE_REQUEST_FORMAT) }))
        val request = Request.Builder()
            .url("$rootUrl/sync/fetch-index")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()
        Log.d(TAG, "索引拉取请求: ${request.url}, dates=${dates.size}")

        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> {
                        Log.d(TAG, "索引拉取响应 404 无索引条目")
                        markDatesAbsent(dates)
                        FetchIndexResult.NotFound
                    }
                    response.isSuccessful -> {
                        val body = response.body?.string()
                        val indexResponse = try {
                            gson.fromJson(body, FetchIndexResponse::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "索引响应解析异常", e)
                            return@use FetchIndexResult.Error(ERROR_MESSAGE)
                        }
                        val index = indexResponse.index.orEmpty()
                        val existingDates = index.mapNotNull { dto ->
                            runCatching { LocalDate.of(dto.year, dto.month, dto.day) }.getOrNull()
                        }
                        indexDao.upsertAll(index.map { dto ->
                            MobileDailyReportIndexEntity(
                                year = dto.year,
                                month = dto.month,
                                day = dto.day,
                                path = dto.path.orEmpty().removePrefix("data/"),
                                fileSize = dto.fileSize,
                                status = 0
                            )
                        })
                        markDatesAbsent(dates.filter { it !in existingDates })
                        Log.d(TAG, "索引拉取成功: 存在 ${existingDates.size} 天")
                        FetchIndexResult.Success(existingDates.sorted())
                    }
                    else -> {
                        Log.w(TAG, "索引拉取响应异常: code=${response.code}")
                        FetchIndexResult.Error(ERROR_MESSAGE)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "索引拉取异常: ${e.javaClass.simpleName}: ${e.message}", e)
            FetchIndexResult.Error(ERROR_MESSAGE)
        }
    }

    private val dataDir: File
        get() = File(context.filesDir, "data")

    private fun reportFileFor(date: LocalDate): File {
        val path = date.format(DATE_PATH_FORMAT) + ".txt"
        return File(context.filesDir, "data/$path")
    }

    override suspend fun clear(dates: List<LocalDate>) {
        withContext(Dispatchers.IO) {
            dates.forEach { date ->
                reportFileFor(date).delete()
                indexDao.deleteByDate(date.year, date.monthValue, date.dayOfMonth)
            }
            Log.d(TAG, "清理完成: ${dates.size} 天")
        }
    }

    override suspend fun clearBefore(cutoff: LocalDate) {
        withContext(Dispatchers.IO) {
            if (dataDir.exists()) {
                dataDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val rel = file.relativeTo(dataDir).path.replace('\\', '/')
                    if (ENTRY_NAME_REGEX.matches(rel)) {
                        val parts = rel.removeSuffix(".txt").split("/")
                        val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                        if (!date.isAfter(cutoff)) {
                            file.delete()
                        }
                    }
                }
            }
            indexDao.getAll().forEach { entry ->
                val date = LocalDate.of(entry.year, entry.month, entry.day)
                if (!date.isAfter(cutoff)) {
                    indexDao.deleteByDate(entry.year, entry.month, entry.day)
                }
            }
            Log.d(TAG, "时间清理完成: cutoff=$cutoff")
        }
    }

    override suspend fun getReportStatus(): Map<LocalDate, Int> = withContext(Dispatchers.IO) {
        indexDao.getAll().associate { entry ->
            LocalDate.of(entry.year, entry.month, entry.day) to entry.status
        }
    }

    private suspend fun markDatesAbsent(dates: List<LocalDate>) {
        val entries = dates.map { date ->
            val existing = indexDao.findByDate(date.year, date.monthValue, date.dayOfMonth)
            existing ?: MobileDailyReportIndexEntity(
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth,
                path = date.format(DATE_PATH_FORMAT) + ".txt",
                fileSize = 0,
                status = -1
            )
        }
        indexDao.upsertAll(entries)
    }

    private fun extractZip(zipFile: File): Set<LocalDate>? {
        return try {
            val extracted = mutableSetOf<LocalDate>()
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (ENTRY_NAME_REGEX.matches(name)) {
                        val target = File(context.filesDir, "data/$name")
                        target.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            target.outputStream().use { input.copyTo(it) }
                        }
                        val parts = name.removeSuffix(".txt").split("/")
                        extracted.add(LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()))
                    } else {
                        Log.w(TAG, "跳过非法 zip 条目: $name")
                    }
                }
            }
            extracted
        } catch (e: Exception) {
            Log.e(TAG, "解压异常: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    private suspend fun updateIndexAfterFetch(dates: List<LocalDate>, extractedDates: Set<LocalDate>) {
        val today = LocalDate.now()
        val entries = dates.map { date ->
            val existing = indexDao.findByDate(date.year, date.monthValue, date.dayOfMonth)
            if (date in extractedDates) {
                val path = date.format(DATE_PATH_FORMAT) + ".txt"
                MobileDailyReportIndexEntity(
                    year = date.year,
                    month = date.monthValue,
                    day = date.dayOfMonth,
                    path = path,
                    fileSize = File(context.filesDir, "data/$path").length(),
                    status = if (date < today) 1 else 0
                )
            } else {
                existing ?: MobileDailyReportIndexEntity(
                    year = date.year,
                    month = date.monthValue,
                    day = date.dayOfMonth,
                    path = date.format(DATE_PATH_FORMAT) + ".txt",
                    fileSize = 0,
                    status = -1
                )
            }
        }
        indexDao.upsertAll(entries)
    }
}
