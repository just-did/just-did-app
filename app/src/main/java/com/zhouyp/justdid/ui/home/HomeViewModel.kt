package com.zhouyp.justdid.ui.home

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhouyp.justdid.domain.model.DailyDisplay
import com.zhouyp.justdid.domain.model.DisplayRow
import com.zhouyp.justdid.domain.model.FetchResult
import com.zhouyp.justdid.domain.model.PushResult
import com.zhouyp.justdid.domain.model.UpdatedIndexEntry
import com.zhouyp.justdid.domain.RecordContentRules
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
    val isPushing: Boolean = false,
    val contentDates: List<LocalDate> = emptyList(),
    val displays: Map<LocalDate, DailyDisplay> = emptyMap(),
    val displayRows: List<DisplayRow> = emptyList(),
    val anchorDate: LocalDate? = null,
    val scrollToTodayTick: Int = 0,
    val historyLoadingIndicatorEnabled: Boolean = false
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

    private val _pushUiEvents = MutableSharedFlow<PushUiEvent>(extraBufferCapacity = 1)
    val pushUiEvents: SharedFlow<PushUiEvent> = _pushUiEvents

    private var fullDates: List<LocalDate> = emptyList()

    companion object {
        private const val CONTENT_CHUNK_SIZE = 500
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            connectionRepository.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        connectionRepository.startPolling()
        refreshDailyReport()
    }

    override fun onStop(owner: LifecycleOwner) {
        connectionRepository.stopPolling()
    }

    fun onInputTextChange(text: String) {
        if (RecordContentRules.hasConsecutiveLineBreaks(text)) {
            _pushUiEvents.tryEmit(PushUiEvent.Toast("不支持连续换行"))
            return
        }
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun record() {
        val content = _uiState.value.inputText.trim()
        if (content.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.saveRecord(content)
            fullDates = homeRepository.getContentDates()
            val today = fullDates.last()
            val display = homeRepository.getDailyDisplay(today)
            _uiState.value = _uiState.value.copy(
                inputText = "",
                contentDates = allDatesDesc(),
                displays = mapOf(today to display),
                anchorDate = today,
                scrollToTodayTick = _uiState.value.scrollToTodayTick + 1,
                historyLoadingIndicatorEnabled = false
            ).withRows()
        }
    }

    private fun allDatesDesc(): List<LocalDate> {
        // 全量日期倒序：今天在首位（列表底部），历史在后（上方）
        return fullDates.sortedDescending()
    }

    private fun buildDisplayRows(
        contentDates: List<LocalDate>,
        displays: Map<LocalDate, DailyDisplay>
    ): List<DisplayRow> {
        val today = LocalDate.now()
        val rows = mutableListOf<DisplayRow>()
        contentDates.forEach { date ->
            val display = displays[date]
            if (display == null) {
                rows.add(DisplayRow.Placeholder(date))
                return@forEach
            }
            // reverseLayout 下数据顺序与视觉顺序相反：
            // 分组按时间倒序排放（视觉上时间升序、同时间日报在前），日期头最后排放（视觉上在内容上方）
            var seq = 0
            display.groups.asReversed().forEach { group ->
                // 组内数据倒序排放（reverseLayout 视觉相反）：视觉上时间头在上、内容按序在下
                val chunksList = group.contents.flatMap { content ->
                    if (content.length > CONTENT_CHUNK_SIZE) {
                        content.chunked(CONTENT_CHUNK_SIZE)
                    } else {
                        listOf(content)
                    }
                }
                chunksList.asReversed().forEach { chunk ->
                    rows.add(
                        DisplayRow.ContentLine(
                            date = date,
                            seq = seq++,
                            text = chunk,
                            source = group.source
                        )
                    )
                }
                rows.add(DisplayRow.TimeHeader(date, seq++, group.time, group.source))
            }
            rows.add(
                DisplayRow.DayHeader(
                    date = date,
                    isEmptyToday = display.groups.isEmpty() && date == today
                )
            )
        }
        return rows
    }

    private fun HomeUiState.withRows(): HomeUiState =
        copy(displayRows = buildDisplayRows(contentDates, displays))

    fun loadDisplay(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.displays.containsKey(date)) return@launch
            val display = homeRepository.getDailyDisplay(date)
            _uiState.value = _uiState.value.copy(
                displays = _uiState.value.displays + (date to display)
            ).withRows()
        }
    }

    fun onAnchorChanged(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            if (state.anchorDate == date) return@launch

            // 滚动中只更新锚点与加载，不做窗口淘汰（避免滚动中重建上方行导致视口跳变）
            _uiState.value = state.copy(anchorDate = date)
            loadDisplay(date)
        }
    }

    fun prefetch(direction: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val anchor = state.anchorDate ?: state.contentDates.firstOrNull() ?: return@launch
            val index = state.contentDates.indexOf(anchor)
            val target = state.contentDates.getOrNull(index + direction) ?: return@launch
            loadDisplay(target)
        }
    }

    fun onHistoryNavigationStarted() {
        val state = _uiState.value
        if (!state.historyLoadingIndicatorEnabled) {
            _uiState.value = state.copy(historyLoadingIndicatorEnabled = true)
        }
    }

    fun evictWindow(visibleDates: Set<LocalDate>) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val anchor = state.anchorDate ?: return@launch
            val windowStart = anchor.minusDays(5)
            val windowEnd = anchor.plusDays(5)
            val filtered = state.displays.filterKeys {
                it in visibleDates || (it in windowStart..windowEnd)
            }
            if (filtered.size != state.displays.size) {
                _uiState.value = state.copy(displays = filtered).withRows()
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val anchor = state.anchorDate
            fullDates = homeRepository.getContentDates()
            val anchorExists = anchor != null && anchor in fullDates
            val target = if (anchorExists) anchor else fullDates.last()
            val display = homeRepository.getDailyDisplay(target)
            _uiState.value = _uiState.value.copy(
                contentDates = allDatesDesc(),
                displays = mapOf(target to display),
                anchorDate = target,
                scrollToTodayTick = if (anchorExists) {
                    state.scrollToTodayTick
                } else {
                    state.scrollToTodayTick + 1
                },
                historyLoadingIndicatorEnabled = false
            ).withRows()
        }
    }
}
