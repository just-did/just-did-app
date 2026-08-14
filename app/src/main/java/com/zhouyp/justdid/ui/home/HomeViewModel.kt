package com.zhouyp.justdid.ui.home

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhouyp.justdid.domain.model.FetchResult
import com.zhouyp.justdid.domain.model.PushResult
import com.zhouyp.justdid.domain.model.RecordGroup
import com.zhouyp.justdid.domain.model.UpdatedIndexEntry
import com.zhouyp.justdid.domain.repository.ConnectionRepository
import com.zhouyp.justdid.domain.repository.DailyReportRepository
import com.zhouyp.justdid.domain.repository.HomeRepository
import com.zhouyp.justdid.domain.repository.PushRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isConnected: Boolean = true,
    val inputText: String = "",
    val todayRecords: List<RecordGroup> = emptyList(),
    val isPushing: Boolean = false
)

sealed interface PushUiEvent {
    data class Toast(val message: String) : PushUiEvent
    data class SuccessDialog(val entries: List<UpdatedIndexEntry>) : PushUiEvent
    data class DiscardDialog(val batchId: String, val message: String) : PushUiEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val connectionRepository: ConnectionRepository,
    private val pushRepository: PushRepository,
    private val dailyReportRepository: DailyReportRepository
) : ViewModel(), DefaultLifecycleObserver {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _pushUiEvents = MutableSharedFlow<PushUiEvent>()
    val pushUiEvents: SharedFlow<PushUiEvent> = _pushUiEvents

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

    fun pushStaging() {
        if (_uiState.value.isPushing) return
        _uiState.value = _uiState.value.copy(isPushing = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val result = pushRepository.push()) {
                    PushResult.NoData -> _pushUiEvents.emit(PushUiEvent.Toast("暂无可推送的数据"))
                    is PushResult.Success -> _pushUiEvents.emit(PushUiEvent.SuccessDialog(result.entries))
                    is PushResult.DiscardableError ->
                        _pushUiEvents.emit(PushUiEvent.DiscardDialog(result.batchId, result.message))
                    is PushResult.Error -> _pushUiEvents.emit(PushUiEvent.Toast(result.message))
                }
            } finally {
                _uiState.value = _uiState.value.copy(isPushing = false)
                loadTodayRecords()
                refreshDailyReport()
            }
        }
    }

    fun discardBatch(batchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pushRepository.discardBatch(batchId)
        }
    }

    fun fetchDailyReports(dates: List<LocalDate>) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = dailyReportRepository.fetch(dates)) {
                FetchResult.Success -> {
                    _pushUiEvents.emit(PushUiEvent.Toast("拉取成功"))
                    refreshDailyReport()
                }
                FetchResult.NotFound -> _pushUiEvents.emit(PushUiEvent.Toast("无文件"))
                is FetchResult.Error -> _pushUiEvents.emit(PushUiEvent.Toast(result.message))
            }
        }
    }

    fun refreshDailyReport() {
        // TODO: 实现日报拉取后的展示刷新
    }
}
