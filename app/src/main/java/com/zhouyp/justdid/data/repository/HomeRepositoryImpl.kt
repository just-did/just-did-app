package com.zhouyp.justdid.data.repository

import android.content.Context
import com.zhouyp.justdid.data.local.db.dao.NoteDao
import com.zhouyp.justdid.data.local.file.FileStorageManager
import com.zhouyp.justdid.data.remote.ApiService
import com.zhouyp.justdid.domain.model.RecordGroup
import com.zhouyp.justdid.domain.repository.HomeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val noteDao: NoteDao,
    private val fileStorageManager: FileStorageManager,
    @ApplicationContext private val context: Context
) : HomeRepository {

    private fun stagingFileFor(date: LocalDate): File {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return File(context.filesDir, "staging/staging-$dateStr.txt")
    }

    private val stagingFileForToday: File
        get() = stagingFileFor(LocalDate.now())

    override suspend fun saveRecord(content: String) {
        withContext(Dispatchers.IO) {
            val timeStr = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm"))

            if (!stagingFileForToday.exists()) {
                fileStorageManager.saveText(stagingFileForToday, "$timeStr\n$content")
                return@withContext
            }

            val lastTime = findLastTime()

            val toAppend = when {
                lastTime != timeStr -> "\n\n$timeStr\n$content"
                else -> "\n$content"
            }

            fileStorageManager.appendText(stagingFileForToday, toAppend)
        }
    }

    override suspend fun getTodayRecords(): List<RecordGroup> {
        return withContext(Dispatchers.IO) {
            if (!stagingFileForToday.exists()) return@withContext emptyList()

            val groups = mutableListOf<RecordGroup>()
            var currentTime: String? = null
            var currentContents = mutableListOf<String>()

            stagingFileForToday.bufferedReader().use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    when {
                        line.matches(Regex("^\\d{2}:\\d{2}$")) -> {
                            flushGroup(currentTime, currentContents, groups)
                            currentTime = line
                            currentContents = mutableListOf()
                        }
                        line.isNotEmpty() -> {
                            if (currentTime != null) {
                                currentContents.add(line)
                            }
                        }
                    }
                    line = reader.readLine()
                }
                flushGroup(currentTime, currentContents, groups)
            }

            groups
        }
    }

    private fun findLastTime(): String? {
        var chunkSize = 50
        var lastTime: String? = null

        while (lastTime == null) {
            val lines = fileStorageManager.readLastNLines(stagingFileForToday, chunkSize)
            for (line in lines.reversed()) {
                if (line.matches(Regex("^\\d{2}:\\d{2}$"))) {
                    lastTime = line
                    break
                }
            }
            if (lines.size < chunkSize) break
            chunkSize += 50
        }

        return lastTime
    }

    private fun flushGroup(
        currentTime: String?,
        contents: List<String>,
        groups: MutableList<RecordGroup>
    ) {
        if (currentTime != null && contents.isNotEmpty()) {
            groups.add(RecordGroup(currentTime, contents.toList()))
        }
    }
}
