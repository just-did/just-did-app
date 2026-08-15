package com.zhouyp.justdid.domain.repository

import com.zhouyp.justdid.domain.model.FetchIndexResult
import com.zhouyp.justdid.domain.model.FetchResult
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface DailyReportRepository {
    val cacheUsage: StateFlow<Long>
    suspend fun refreshCacheUsage()
    suspend fun fetch(dates: List<LocalDate>): FetchResult
    suspend fun fetchIndex(dates: List<LocalDate>): FetchIndexResult
    suspend fun clear(dates: List<LocalDate>)
    suspend fun clearBefore(cutoff: LocalDate)
    suspend fun getReportStatus(): Map<LocalDate, Int>
}
