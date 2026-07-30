package com.zhouyp.justdid.data.repository

import com.zhouyp.justdid.data.local.db.dao.NoteDao
import com.zhouyp.justdid.data.local.file.FileStorageManager
import com.zhouyp.justdid.data.remote.ApiService
import com.zhouyp.justdid.domain.repository.HomeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val noteDao: NoteDao,
    private val fileStorageManager: FileStorageManager
) : HomeRepository
