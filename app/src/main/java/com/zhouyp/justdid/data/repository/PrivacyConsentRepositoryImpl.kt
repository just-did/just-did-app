package com.zhouyp.justdid.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zhouyp.justdid.domain.model.PrivacyConsentStatus
import com.zhouyp.justdid.domain.model.PrivacyPolicyConfig
import com.zhouyp.justdid.domain.repository.PrivacyConsentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PrivacyConsentRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PrivacyConsentRepository {

    private companion object {
        val KEY_ACCEPTED_PRIVACY_POLICY_VERSION =
            stringPreferencesKey("accepted_privacy_policy_version")
    }

    override val consentStatus: Flow<PrivacyConsentStatus> = dataStore.data.map { preferences ->
        resolvePrivacyConsentStatus(preferences[KEY_ACCEPTED_PRIVACY_POLICY_VERSION])
    }

    override suspend fun acceptCurrentPolicy() {
        dataStore.edit { preferences ->
            preferences[KEY_ACCEPTED_PRIVACY_POLICY_VERSION] = PrivacyPolicyConfig.VERSION
        }
    }

    override suspend fun withdrawConsent() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCEPTED_PRIVACY_POLICY_VERSION)
        }
    }
}

internal fun resolvePrivacyConsentStatus(acceptedVersion: String?): PrivacyConsentStatus =
    if (acceptedVersion == PrivacyPolicyConfig.VERSION) {
        PrivacyConsentStatus.ACCEPTED
    } else {
        PrivacyConsentStatus.REQUIRED
    }
