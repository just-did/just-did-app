package com.zhouyp.justdid.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "mobile_daliy_report_index", primaryKeys = ["year", "month", "day"])
data class MobileDailyReportIndexEntity(
    val year: Int,
    val month: Int,
    val day: Int,
    val path: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    val status: Int = 0
)
