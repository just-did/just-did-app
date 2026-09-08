package com.zhouyp.justdid.domain.repository

import com.zhouyp.justdid.domain.model.PrivacyConsentStatus
import kotlinx.coroutines.flow.Flow

interface PrivacyConsentRepository {
    val consentStatus: Flow<PrivacyConsentStatus>

    suspend fun acceptCurrentPolicy()

    suspend fun withdrawConsent()
}
