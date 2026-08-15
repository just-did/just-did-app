package com.zhouyp.justdid.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.zhouyp.justdid.domain.model.AppConfig
import com.zhouyp.justdid.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ConfigRepository {

    companion object {
        private val KEY_CACHE_TOTAL_BYTES = longPreferencesKey("cache_total_bytes")
    }

    override suspend fun getConfig(): AppConfig {
        val stored = runCatching { dataStore.data.first()[KEY_CACHE_TOTAL_BYTES] }.getOrNull()
        return AppConfig(cacheTotalBytes = stored ?: AppConfig.DEFAULT.cacheTotalBytes)
    }
}
