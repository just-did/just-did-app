package com.zhouyp.justdid.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zhouyp.justdid.data.local.db.entity.NoteEntity

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity)
}
