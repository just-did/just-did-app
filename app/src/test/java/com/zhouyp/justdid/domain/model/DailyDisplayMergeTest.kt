package com.zhouyp.justdid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyDisplayMergeTest {

    private fun group(time: String, source: RecordSource, content: String = "内容") =
        RecordGroup(time, listOf(content), source)

    @Test
    fun `合并按时间升序排序`() {
        val report = listOf(
            group("10:20", RecordSource.REPORT),
            group("08:05", RecordSource.REPORT)
        )
        val staging = listOf(
            group("09:54", RecordSource.STAGING)
        )

        val merged = DailyDisplay.mergeGroups(report, staging)

        assertEquals(listOf("08:05", "09:54", "10:20"), merged.map { it.time })
    }

    @Test
    fun `同时间分组日报在前`() {
        val report = listOf(group("10:20", RecordSource.REPORT, "日报内容"))
        val staging = listOf(group("10:20", RecordSource.STAGING, "暂存内容"))

        val merged = DailyDisplay.mergeGroups(report, staging)

        assertEquals(2, merged.size)
        assertEquals(RecordSource.REPORT, merged[0].source)
        assertEquals(RecordSource.STAGING, merged[1].source)
    }

    @Test
    fun `仅日报来源`() {
        val report = listOf(group("09:54", RecordSource.REPORT))

        val merged = DailyDisplay.mergeGroups(report, emptyList())

        assertEquals(1, merged.size)
        assertEquals(RecordSource.REPORT, merged[0].source)
    }

    @Test
    fun `仅暂存来源`() {
        val staging = listOf(group("09:54", RecordSource.STAGING))

        val merged = DailyDisplay.mergeGroups(emptyList(), staging)

        assertEquals(1, merged.size)
        assertEquals(RecordSource.STAGING, merged[0].source)
    }

    @Test
    fun `同来源同时间保持原有顺序`() {
        val report = listOf(
            group("10:20", RecordSource.REPORT, "第一条"),
            group("10:20", RecordSource.REPORT, "第二条")
        )

        val merged = DailyDisplay.mergeGroups(report, emptyList())

        assertEquals(listOf("第一条", "第二条"), merged.map { it.contents.first() })
    }
}
