package com.zhouyp.justdid.domain.model

import java.time.LocalDate

sealed interface FetchIndexResult {
    data class Success(val existingDates: List<LocalDate>) : FetchIndexResult
    object NotFound : FetchIndexResult
    data class Error(val message: String) : FetchIndexResult
}
