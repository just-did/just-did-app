package com.zhouyp.justdid.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zhouyp.justdid.data.local.db.dao.DailyReportIndexDao
import com.zhouyp.justdid.data.local.db.dao.NoteDao
import com.zhouyp.justdid.data.local.db.entity.MobileDailyReportIndexEntity
import com.zhouyp.justdid.data.local.db.entity.NoteEntity

@Database(
    entities = [NoteEntity::class, MobileDailyReportIndexEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun dailyReportIndexDao(): DailyReportIndexDao
}
