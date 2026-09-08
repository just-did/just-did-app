package com.zhouyp.justdid.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhouyp.justdid.domain.model.PrivacyConsentStatus
import com.zhouyp.justdid.domain.repository.PrivacyConsentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PrivacyConsentUiState {
    data object Loading : PrivacyConsentUiState
    data object Required : PrivacyConsentUiState
    data object Declined : PrivacyConsentUiState
    data object Accepted : PrivacyConsentUiState
}

@HiltViewModel
class PrivacyConsentViewModel @Inject constructor(
    private val privacyConsentRepository: PrivacyConsentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrivacyConsentUiState>(PrivacyConsentUiState.Loading)
    val uiState: StateFlow<PrivacyConsentUiState> = _uiState

    init {
        viewModelScope.launch {
            privacyConsentRepository.consentStatus.collect { status ->
                _uiState.value = when (status) {
                    PrivacyConsentStatus.REQUIRED -> PrivacyConsentUiState.Required
                    PrivacyConsentStatus.ACCEPTED -> PrivacyConsentUiState.Accepted
                }
            }
        }
    }

    fun accept() {
        viewModelScope.launch {
            privacyConsentRepository.acceptCurrentPolicy()
        }
    }

    fun decline() {
        _uiState.value = PrivacyConsentUiState.Declined
    }

    fun chooseAgain() {
        _uiState.value = PrivacyConsentUiState.Required
    }
}
