package com.zhouyp.justdid.ui.settings

import androidx.lifecycle.ViewModel
import com.zhouyp.justdid.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

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
