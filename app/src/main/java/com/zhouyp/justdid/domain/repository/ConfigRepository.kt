package com.zhouyp.justdid.domain.repository

import com.zhouyp.justdid.domain.model.AppConfig

interface ConfigRepository {
    suspend fun getConfig(): AppConfig
}
