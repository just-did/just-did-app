package com.zhouyp.justdid.domain.model

data class AppConfig(
    val cacheTotalBytes: Long = DEFAULT.cacheTotalBytes
) {
    companion object {
        val DEFAULT = AppConfig(cacheTotalBytes = 1073741824L)
    }
}
