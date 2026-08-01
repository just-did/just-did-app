package com.zhouyp.justdid.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zhouyp.justdid.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val KEY_MASTER_ROOT_URL = stringPreferencesKey("master_root_url")
    }

    override suspend fun getMasterRootUrl(): String? {
        return dataStore.data.first()[KEY_MASTER_ROOT_URL]
    }

    override suspend fun setMasterRootUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[KEY_MASTER_ROOT_URL] = url
        }
    }
}
