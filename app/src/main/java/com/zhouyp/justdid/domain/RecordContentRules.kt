package com.zhouyp.justdid.domain

object RecordContentRules {
    private val consecutiveLineBreaks = Regex("\n{2,}")

    fun normalizeLineEndings(content: String): String =
        content.replace("\r\n", "\n").replace('\r', '\n')

    fun hasConsecutiveLineBreaks(content: String): Boolean =
        consecutiveLineBreaks.containsMatchIn(normalizeLineEndings(content))

    fun normalizeForStorage(content: String): String =
        normalizeLineEndings(content).replace(consecutiveLineBreaks, "\n")

    fun formatForAppend(
        content: String,
        currentTime: String,
        lastTime: String?,
        fileExists: Boolean
    ): String {
        val normalizedContent = normalizeForStorage(content)
        return when {
            !fileExists -> "$currentTime\n$normalizedContent"
            lastTime != currentTime -> "\n\n$currentTime\n$normalizedContent"
            else -> "\n$normalizedContent"
        }
    }
}
