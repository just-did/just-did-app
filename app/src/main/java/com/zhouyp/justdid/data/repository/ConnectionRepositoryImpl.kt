package com.zhouyp.justdid.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.zhouyp.justdid.domain.repository.ConnectionRepository
import com.zhouyp.justdid.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ConnectionRepository {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    private val healthCheckClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ConnectionRepo"
    }

    init {
        scope.launch {
            val cachedUrl = settingsRepository.getMasterRootUrl()
            Log.d(TAG, "初始化 - 缓存URL: $cachedUrl")
            if (cachedUrl != null) {
                val success = checkHealth(cachedUrl)
                _isConnected.value = success
                Log.d(TAG, "初始化 - 健康检查结果: $success")
                if (success) {
                    startPollingInternal()
                } else {
                    showToast("连接失败")
                }
            }
        }
    }

    override suspend fun connectToMaster(url: String): Boolean {
        Log.d(TAG, "扫码连接 - URL: $url")
        if (!isValidUrl(url)) {
            Log.w(TAG, "扫码连接 - 无效URL")
            showToast("二维码有误，连接失败")
            return false
        }

        settingsRepository.setMasterRootUrl(url)
        val success = checkHealth(url)
        _isConnected.value = success
        Log.d(TAG, "扫码连接 - 健康检查结果: $success")
        if (success) {
            startPollingInternal()
        } else {
            showToast("连接失败")
        }
        return success
    }

    override fun startPolling() {
        if (_isConnected.value) {
            startPollingInternal()
        }
    }

    override fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun startPollingInternal() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(60_000)
                val url = settingsRepository.getMasterRootUrl() ?: break
                if (!checkHealth(url)) {
                    _isConnected.value = false
                    showToast("连接失败")
                    break
                }
            }
        }
    }

    private suspend fun checkHealth(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fullUrl = "$url/health"
                Log.d(TAG, "健康检查请求: $fullUrl")
                val request = Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build()
                healthCheckClient.newCall(request).execute().use { response ->
                    Log.d(TAG, "健康检查响应: code=${response.code}, message=${response.message}")
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e(TAG, "健康检查异常: ${e.javaClass.simpleName}: ${e.message}", e)
                false
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
