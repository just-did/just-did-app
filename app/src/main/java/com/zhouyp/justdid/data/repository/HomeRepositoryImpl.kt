package com.zhouyp.justdid.data.repository

import android.content.Context
import com.zhouyp.justdid.data.local.db.dao.DailyReportIndexDao
import com.zhouyp.justdid.data.local.file.FileStorageManager
import com.zhouyp.justdid.domain.model.DailyDisplay
import com.zhouyp.justdid.domain.model.RecordGroup
import com.zhouyp.justdid.domain.model.RecordSource
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
    private val indexDao: DailyReportIndexDao,
    private val fileStorageManager: FileStorageManager,
    @ApplicationContext private val context: Context
) : HomeRepository {

    private val stagingDir: File
        get() = File(context.filesDir, "staging")

    private val stagingFileNamePattern = Regex("^staging-(\\d{8})\\.txt$")

    private fun stagingFileFor(date: LocalDate): File {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return File(stagingDir, "staging-$dateStr.txt")
    }

    private val stagingFileForToday: File
        get() = stagingFileFor(LocalDate.now())

    private fun reportFileFor(date: LocalDate): File {
        val path = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + ".txt"
        return File(context.filesDir, "data/$path")
    }

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

    override suspend fun getContentDates(): List<LocalDate> {
        return withContext(Dispatchers.IO) {
            val dates = mutableSetOf<LocalDate>()

            stagingDir.listFiles().orEmpty().forEach { file ->
                stagingFileNamePattern.matchEntire(file.name)?.let { match ->
                    val dateStr = match.groupValues[1]
                    dates.add(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd")))
                }
            }

            indexDao.getAll().forEach { entry ->
                val date = LocalDate.of(entry.year, entry.month, entry.day)
                if (reportFileFor(date).exists()) {
                    dates.add(date)
                }
            }

            dates.add(LocalDate.now())
            dates.sorted()
        }
    }

    override suspend fun getDailyDisplay(date: LocalDate): DailyDisplay {
        return withContext(Dispatchers.IO) {
            val entry = indexDao.findByDate(date.year, date.monthValue, date.dayOfMonth)
            val reportGroups = if (entry != null) {
                parseGroups(reportFileFor(date), RecordSource.REPORT)
            } else {
                emptyList()
            }
            val stagingGroups = parseGroups(stagingFileFor(date), RecordSource.STAGING)
            DailyDisplay(date, DailyDisplay.mergeGroups(reportGroups, stagingGroups))
        }
    }

    private fun parseGroups(file: File, source: RecordSource): List<RecordGroup> {
        if (!file.exists()) return emptyList()

        val groups = mutableListOf<RecordGroup>()
        var currentTime: String? = null
        var currentContents = mutableListOf<String>()

        file.bufferedReader().use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                when {
                    line.matches(Regex("^\\d{2}:\\d{2}$")) -> {
                        flushGroup(currentTime, currentContents, groups, source)
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
            flushGroup(currentTime, currentContents, groups, source)
        }

        return groups
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
        groups: MutableList<RecordGroup>,
        source: RecordSource
    ) {
        if (currentTime != null && contents.isNotEmpty()) {
            groups.add(RecordGroup(currentTime, contents.toList(), source))
        }
    }
}
