package com.zhouyp.justdid.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordContentRulesTest {
    @Test
    fun `识别不同格式的连续换行`() {
        assertTrue(RecordContentRules.hasConsecutiveLineBreaks("第一行\n\n第二行"))
        assertTrue(RecordContentRules.hasConsecutiveLineBreaks("第一行\r\n\r\n第二行"))
        assertTrue(RecordContentRules.hasConsecutiveLineBreaks("第一行\r\r第二行"))
    }

    @Test
    fun `单个换行保持有效`() {
        assertFalse(RecordContentRules.hasConsecutiveLineBreaks("第一行\n第二行"))
        assertEquals("第一行\n第二行", RecordContentRules.normalizeForStorage("第一行\r\n第二行"))
    }

    @Test
    fun `保存前压缩连续换行`() {
        assertEquals(
            "第一行\n第二行\n第三行",
            RecordContentRules.normalizeForStorage("第一行\n\n\n第二行\r\r第三行")
        )
    }

    @Test
    fun `首条记录写入时规范化连续换行`() {
        assertEquals(
            "08:10\n第一行\n第二行",
            RecordContentRules.formatForAppend("第一行\n\n第二行", "08:10", null, false)
        )
    }

    @Test
    fun `同分钟追加保持一个记录分隔换行`() {
        assertEquals(
            "\n第一行\n第二行",
            RecordContentRules.formatForAppend("第一行\r\n\r\n第二行", "10:20", "10:20", true)
        )
    }

    @Test
    fun `跨分钟追加保持时间块分隔`() {
        assertEquals(
            "\n\n10:30\n新内容",
            RecordContentRules.formatForAppend("新内容", "10:30", "10:20", true)
        )
    }
}
