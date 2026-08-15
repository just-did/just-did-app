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
        // 与 DayHeader 同 key：内容加载替换占位时，滚动锚定无缝转移到日期头，避免视口跳变
        override val key: String get() = "h:$date"
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
