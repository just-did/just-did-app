package com.zhouyp.justdid.data.repository

import com.zhouyp.justdid.domain.model.PrivacyConsentStatus
import com.zhouyp.justdid.domain.model.PrivacyPolicyConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacyConsentStatusTest {

    @Test
    fun `隐私政策配置与当前发布版本一致`() {
        assertEquals("2026-08-28", PrivacyPolicyConfig.VERSION)
        assertEquals("https://just-did.zhouyp.top/privacy.html", PrivacyPolicyConfig.URL)
    }

    @Test
    fun `未保存版本时需要用户同意`() {
        assertEquals(PrivacyConsentStatus.REQUIRED, resolvePrivacyConsentStatus(null))
    }

    @Test
    fun `保存版本与当前版本一致时视为已同意`() {
        assertEquals(
            PrivacyConsentStatus.ACCEPTED,
            resolvePrivacyConsentStatus(PrivacyPolicyConfig.VERSION)
        )
    }

    @Test
    fun `保存版本过期时需要重新同意`() {
        assertEquals(
            PrivacyConsentStatus.REQUIRED,
            resolvePrivacyConsentStatus("2026-01-01")
        )
    }
}
