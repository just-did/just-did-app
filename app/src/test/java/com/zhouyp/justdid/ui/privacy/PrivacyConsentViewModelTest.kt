package com.zhouyp.justdid.ui.privacy

import com.zhouyp.justdid.MainDispatcherRule
import com.zhouyp.justdid.domain.model.PrivacyConsentStatus
import com.zhouyp.justdid.domain.repository.PrivacyConsentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyConsentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `未同意时可拒绝并重新选择`() = runTest {
        val repository = FakePrivacyConsentRepository(PrivacyConsentStatus.REQUIRED)
        val viewModel = PrivacyConsentViewModel(repository)
        runCurrent()

        assertEquals(PrivacyConsentUiState.Required, viewModel.uiState.value)
        viewModel.decline()
        assertEquals(PrivacyConsentUiState.Declined, viewModel.uiState.value)
        viewModel.chooseAgain()
        assertEquals(PrivacyConsentUiState.Required, viewModel.uiState.value)
    }

    @Test
    fun `主动同意后进入已同意状态`() = runTest {
        val repository = FakePrivacyConsentRepository(PrivacyConsentStatus.REQUIRED)
        val viewModel = PrivacyConsentViewModel(repository)
        runCurrent()

        viewModel.accept()
        runCurrent()

        assertEquals(PrivacyConsentUiState.Accepted, viewModel.uiState.value)
    }
}

private class FakePrivacyConsentRepository(
    initialStatus: PrivacyConsentStatus
) : PrivacyConsentRepository {
    private val status = MutableStateFlow(initialStatus)
    override val consentStatus = status.asStateFlow()

    override suspend fun acceptCurrentPolicy() {
        status.value = PrivacyConsentStatus.ACCEPTED
    }

    override suspend fun withdrawConsent() {
        status.value = PrivacyConsentStatus.REQUIRED
    }
}
