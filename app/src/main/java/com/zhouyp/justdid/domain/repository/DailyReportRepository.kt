package com.zhouyp.justdid.domain.repository

import com.zhouyp.justdid.domain.model.FetchIndexResult
import com.zhouyp.justdid.domain.model.FetchResult
import java.time.LocalDate

interface DailyReportRepository {
    suspend fun fetch(dates: List<LocalDate>): FetchResult
    suspend fun fetchIndex(dates: List<LocalDate>): FetchIndexResult
}
