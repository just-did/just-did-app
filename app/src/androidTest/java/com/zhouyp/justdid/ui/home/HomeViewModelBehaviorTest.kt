package com.zhouyp.justdid.ui.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zhouyp.justdid.domain.model.DailyDisplay
import com.zhouyp.justdid.domain.model.FetchIndexResult
import com.zhouyp.justdid.domain.model.FetchResult
import com.zhouyp.justdid.domain.model.PushResult
import com.zhouyp.justdid.domain.repository.ConnectionRepository
import com.zhouyp.justdid.domain.repository.DailyReportRepository
import com.zhouyp.justdid.domain.repository.HomeRepository
import com.zhouyp.justdid.domain.repository.PushRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeViewModelBehaviorTest {
    @Test
    fun consecutiveLineBreaksAreRejectedWithOneEvent() = runBlocking {
        val viewModel = createViewModel()
        viewModel.onInputTextChange("第一行")
        val event = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.pushUiEvents.first()
        }

        viewModel.onInputTextChange("第一行\n\n第二行")

        assertEquals("第一行", viewModel.uiState.value.inputText)
        assertEquals(PushUiEvent.Toast("不支持连续换行"), event.await())
        assertNull(withTimeoutOrNull(100) { viewModel.pushUiEvents.first() })
    }

    @Test
    fun singleLineBreakIsAccepted() {
        val viewModel = createViewModel()

        viewModel.onInputTextChange("第一行\n第二行")

        assertEquals("第一行\n第二行", viewModel.uiState.value.inputText)
    }

    @Test
    fun refreshResetsHistoryLoadingIndicator() = runBlocking {
        val viewModel = createViewModel()
        viewModel.onHistoryNavigationStarted()
        assertTrue(viewModel.uiState.value.historyLoadingIndicatorEnabled)

        viewModel.refreshDailyReport()

        withTimeout(2_000) {
            while (viewModel.uiState.value.historyLoadingIndicatorEnabled) {
                delay(10)
            }
        }
        assertFalse(viewModel.uiState.value.historyLoadingIndicatorEnabled)
    }

    private fun createViewModel(): HomeViewModel = HomeViewModel(
        homeRepository = FakeHomeRepository(),
        connectionRepository = FakeConnectionRepository(),
        pushRepository = FakePushRepository(),
        dailyReportRepository = FakeDailyReportRepository()
    )

    private class FakeHomeRepository : HomeRepository {
        override suspend fun saveRecord(content: String) = Unit
        override suspend fun getContentDates(): List<LocalDate> = listOf(LocalDate.now())
        override suspend fun getDailyDisplay(date: LocalDate): DailyDisplay =
            DailyDisplay(date, emptyList())
    }

    private class FakeConnectionRepository : ConnectionRepository {
        override val isConnected = MutableStateFlow(false)
        override suspend fun connectToMaster(url: String): Boolean = true
        override fun startPolling() = Unit
        override fun stopPolling() = Unit
    }

    private class FakePushRepository : PushRepository {
        override suspend fun push(): PushResult = PushResult.NoData
        override suspend fun discardBatch(batchId: String) = Unit
    }

    private class FakeDailyReportRepository : DailyReportRepository {
        override val cacheUsage = MutableStateFlow(0L)
        override suspend fun refreshCacheUsage() = Unit
        override suspend fun fetch(dates: List<LocalDate>): FetchResult = FetchResult.Success
        override suspend fun fetchIndex(dates: List<LocalDate>): FetchIndexResult =
            FetchIndexResult.Success(emptyList())
        override suspend fun clear(dates: List<LocalDate>) = Unit
        override suspend fun clearBefore(cutoff: LocalDate) = Unit
        override suspend fun getReportStatus(): Map<LocalDate, Int> = emptyMap()
    }
}
