package com.zhouyp.justdid.domain.model

import java.time.LocalDate

data class DailyDisplay(
    val date: LocalDate,
    val groups: List<RecordGroup>
) {
    companion object {
        fun mergeGroups(
            reportGroups: List<RecordGroup>,
            stagingGroups: List<RecordGroup>
        ): List<RecordGroup> {
            // sortedBy 稳定排序：同时间分组保持日报在前、暂存在后
            return (reportGroups + stagingGroups).sortedBy { it.time }
        }
    }
}
