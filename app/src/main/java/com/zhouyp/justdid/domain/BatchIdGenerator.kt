package com.zhouyp.justdid.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchIdGenerator @Inject constructor() {

    companion object {
        private const val PREFIX = "xxx"
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }

    fun generate(): String {
        return PREFIX + LocalDateTime.now().format(TIMESTAMP_FORMAT)
    }

    fun isBatchId(name: String): Boolean {
        return name.startsWith(PREFIX)
    }
}
