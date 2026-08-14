package com.zhouyp.justdid.domain.repository

import com.zhouyp.justdid.domain.model.PushResult

interface PushRepository {
    suspend fun push(): PushResult
    suspend fun discardBatch(batchId: String)
}
