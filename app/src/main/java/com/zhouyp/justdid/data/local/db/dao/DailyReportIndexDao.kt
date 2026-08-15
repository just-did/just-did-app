package com.zhouyp.justdid.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zhouyp.justdid.data.local.db.entity.MobileDailyReportIndexEntity

@Dao
interface DailyReportIndexDao {

    @Upsert
    suspend fun upsertAll(entries: List<MobileDailyReportIndexEntity>)

    @Query("SELECT * FROM mobile_daliy_report_index WHERE year = :year AND month = :month AND day = :day")
    suspend fun findByDate(year: Int, month: Int, day: Int): MobileDailyReportIndexEntity?

    @Query("SELECT * FROM mobile_daliy_report_index ORDER BY year, month, day")
    suspend fun getAll(): List<MobileDailyReportIndexEntity>
}
