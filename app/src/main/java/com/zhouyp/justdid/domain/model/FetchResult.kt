package com.zhouyp.justdid.domain.model

sealed interface FetchResult {
    data object Success : FetchResult
    data object NotFound : FetchResult
    data class Error(val message: String) : FetchResult
}
