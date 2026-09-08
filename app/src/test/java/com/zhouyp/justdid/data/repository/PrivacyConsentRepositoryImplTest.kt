package com.zhouyp.justdid.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.zhouyp.justdid.domain.model.PrivacyConsentStatus
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PrivacyConsentRepositoryImplTest {
    private lateinit var dataStoreFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var repository: PrivacyConsentRepositoryImpl

    @Before
    fun setUp() {
        dataStoreFile = File.createTempFile("privacy-consent", ".preferences_pb").apply {
            delete()
        }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repository = PrivacyConsentRepositoryImpl(
            PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
    }

    @Test
    fun `同意与撤回会更新持久化状态`() = runBlocking {
        assertEquals(PrivacyConsentStatus.REQUIRED, repository.consentStatus.first())

        repository.acceptCurrentPolicy()
        assertEquals(PrivacyConsentStatus.ACCEPTED, repository.consentStatus.first())

        repository.withdrawConsent()
        assertEquals(PrivacyConsentStatus.REQUIRED, repository.consentStatus.first())
    }
}
