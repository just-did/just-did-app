package com.zhouyp.justdid.domain.repository

import com.zhouyp.justdid.domain.model.RecordGroup

interface HomeRepository {
    suspend fun saveRecord(content: String)
    suspend fun getTodayRecords(): List<RecordGroup>
}
