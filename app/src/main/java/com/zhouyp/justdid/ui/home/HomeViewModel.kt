package com.zhouyp.justdid.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhouyp.justdid.domain.model.RecordGroup
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
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadTodayRecords()
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
}
