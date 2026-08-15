package com.zhouyp.justdid.domain.repository

import com.zhouyp.justdid.domain.model.DailyDisplay
import java.time.LocalDate

interface HomeRepository {
    suspend fun saveRecord(content: String)
    suspend fun getContentDates(): List<LocalDate>
    suspend fun getDailyDisplay(date: LocalDate): DailyDisplay
}
