package com.zhouyp.justdid.domain.repository

interface SettingsRepository {
    suspend fun getMasterRootUrl(): String?
    suspend fun setMasterRootUrl(url: String)
}
