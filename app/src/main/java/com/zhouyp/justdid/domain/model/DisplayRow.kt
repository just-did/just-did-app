package com.zhouyp.justdid.domain.model

import java.time.LocalDate

sealed interface DisplayRow {
    val date: LocalDate
    val key: String

    data class DayHeader(
        override val date: LocalDate,
        val isEmptyToday: Boolean = false
    ) : DisplayRow {
        override val key: String get() = "h:$date"
    }

    data class Placeholder(override val date: LocalDate) : DisplayRow {
        override val key: String get() = "p:$date"
    }

    data class TimeHeader(
        override val date: LocalDate,
        val seq: Int,
        val time: String,
        val source: RecordSource
    ) : DisplayRow {
        override val key: String get() = "r:$date:$seq"
    }

    data class ContentLine(
        override val date: LocalDate,
        val seq: Int,
        val text: String,
        val source: RecordSource
    ) : DisplayRow {
        override val key: String get() = "r:$date:$seq"
    }
}
