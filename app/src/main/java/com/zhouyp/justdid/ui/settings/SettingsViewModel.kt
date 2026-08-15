package com.zhouyp.justdid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhouyp.justdid.domain.model.FetchIndexResult
import com.zhouyp.justdid.domain.model.FetchResult
import com.zhouyp.justdid.domain.repository.ConnectionRepository
import com.zhouyp.justdid.domain.repository.DailyReportRepository
import com.zhouyp.justdid.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class CalendarMode { CLEAR, FETCH }

data class SettingsUiState(
    val isConnected: Boolean = true,
    val mode: CalendarMode = CalendarMode.CLEAR,
    val selectedDates: Set<LocalDate> = emptySet(),
    val currentMonth: YearMonth = YearMonth.now(),
    val storageUsedPercent: Float = 35f,
    val showDropdownMenu: Boolean = false,
    val isFetching: Boolean = false,
    val isClearing: Boolean = false
)

sealed interface SettingsUiEvent {
    data class Toast(val message: String) : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val connectionRepository: ConnectionRepository,
    private val dailyReportRepository: DailyReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _settingsUiEvents = MutableSharedFlow<SettingsUiEvent>()
    val settingsUiEvents: SharedFlow<SettingsUiEvent> = _settingsUiEvents

    init {
        viewModelScope.launch(Dispatchers.IO) {
            connectionRepository.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
        }
    }

    fun connectToMaster(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            connectionRepository.connectToMaster(url)
        }
    }

    fun toggleMode(mode: CalendarMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun selectDate(date: LocalDate) {
        val current = _uiState.value.selectedDates
        val updated = if (date in current) {
            current - date
        } else {
            current + date
        }
        _uiState.value = _uiState.value.copy(selectedDates = updated)
    }

    fun navigateMonth(direction: Int) {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.plusMonths(direction.toLong())
        )
    }

    fun toggleDropdownMenu(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDropdownMenu = show)
    }

    fun fetchSelected() {
        if (_uiState.value.isFetching) return
        val dates = _uiState.value.selectedDates.sorted()
        if (dates.isEmpty()) {
            viewModelScope.launch {
                _settingsUiEvents.emit(SettingsUiEvent.Toast("请先选择日期"))
            }
            return
        }

        _uiState.value = _uiState.value.copy(isFetching = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val indexResult = dailyReportRepository.fetchIndex(dates)) {
                    is FetchIndexResult.Success -> {
                        when (val fetchResult = dailyReportRepository.fetch(indexResult.existingDates)) {
                            FetchResult.Success -> {
                                _settingsUiEvents.emit(SettingsUiEvent.Toast("拉取成功"))
                                _uiState.value = _uiState.value.copy(selectedDates = emptySet())
                            }
                            FetchResult.NotFound ->
                                _settingsUiEvents.emit(SettingsUiEvent.Toast("无文件"))
                            is FetchResult.Error ->
                                _settingsUiEvents.emit(SettingsUiEvent.Toast(fetchResult.message))
                        }
                    }
                    FetchIndexResult.NotFound ->
                        _settingsUiEvents.emit(SettingsUiEvent.Toast("无文件"))
                    is FetchIndexResult.Error ->
                        _settingsUiEvents.emit(SettingsUiEvent.Toast(indexResult.message))
                }
            } finally {
                _uiState.value = _uiState.value.copy(isFetching = false)
            }
        }
    }

    fun clearSelected() {
        if (_uiState.value.isClearing) return
        val dates = _uiState.value.selectedDates.sorted()
        if (dates.isEmpty()) {
            viewModelScope.launch {
                _settingsUiEvents.emit(SettingsUiEvent.Toast("请先选择日期"))
            }
            return
        }

        _uiState.value = _uiState.value.copy(isClearing = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dailyReportRepository.clear(dates)
                _settingsUiEvents.emit(SettingsUiEvent.Toast("清理成功"))
                _uiState.value = _uiState.value.copy(selectedDates = emptySet())
            } finally {
                _uiState.value = _uiState.value.copy(isClearing = false)
            }
        }
    }

    fun clearBefore(cutoff: LocalDate) {
        if (_uiState.value.isClearing) return
        _uiState.value = _uiState.value.copy(isClearing = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dailyReportRepository.clearBefore(cutoff)
                _settingsUiEvents.emit(SettingsUiEvent.Toast("清理成功"))
            } finally {
                _uiState.value = _uiState.value.copy(isClearing = false)
            }
        }
    }

    fun toggleSelectAllInMonth() {
        val current = _uiState.value
        val month = current.currentMonth
        val today = LocalDate.now()
        val allDatesInMonth = (1..month.lengthOfMonth())
            .map { month.atDay(it) }
            .filter { !it.isAfter(today) }
            .toSet()
        val selectedInMonth = current.selectedDates intersect allDatesInMonth

        val updated = if (selectedInMonth.size == allDatesInMonth.size) {
            // 全选状态 → 取消全部
            current.selectedDates - allDatesInMonth
        } else {
            // 未选或部分选 → 全选
            current.selectedDates + allDatesInMonth
        }
        _uiState.value = current.copy(selectedDates = updated)
    }
}
