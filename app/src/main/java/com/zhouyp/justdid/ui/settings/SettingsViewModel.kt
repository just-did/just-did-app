package com.zhouyp.justdid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhouyp.justdid.domain.repository.ConnectionRepository
import com.zhouyp.justdid.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
    val showDropdownMenu: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

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

    fun toggleSelectAllInMonth() {
        val current = _uiState.value
        val month = current.currentMonth
        val allDatesInMonth = (1..month.lengthOfMonth())
            .map { month.atDay(it) }
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
