package com.zhouyp.justdid.ui.home

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhouyp.justdid.domain.model.RecordGroup
import com.zhouyp.justdid.domain.repository.ConnectionRepository
import com.zhouyp.justdid.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isConnected: Boolean = true,
    val inputText: String = "",
    val todayRecords: List<RecordGroup> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val connectionRepository: ConnectionRepository
) : ViewModel(), DefaultLifecycleObserver {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadTodayRecords()
        viewModelScope.launch(Dispatchers.IO) {
            connectionRepository.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        connectionRepository.startPolling()
    }

    override fun onStop(owner: LifecycleOwner) {
        connectionRepository.stopPolling()
    }

    fun onInputTextChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun record() {
        val content = _uiState.value.inputText.trim()
        if (content.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.saveRecord(content)
            _uiState.value = _uiState.value.copy(inputText = "")
            loadTodayRecords()
        }
    }

    fun loadTodayRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = homeRepository.getTodayRecords()
            _uiState.value = _uiState.value.copy(todayRecords = records)
        }
    }

    fun connectToMaster(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            connectionRepository.connectToMaster(url)
        }
    }
}
