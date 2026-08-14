package com.zhouyp.justdid.domain.model

sealed interface PushResult {
    data object NoData : PushResult
    data class Success(val entries: List<UpdatedIndexEntry>) : PushResult
    data class DiscardableError(val batchId: String, val message: String) : PushResult
    data class Error(val message: String) : PushResult
}

data class UpdatedIndexEntry(
    val year: Int,
    val month: Int,
    val day: Int,
    val path: String,
    val fileSize: Long
)
