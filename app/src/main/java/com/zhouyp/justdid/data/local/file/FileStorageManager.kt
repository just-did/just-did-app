package com.zhouyp.justdid.data.local.file

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageManager @Inject constructor() {

    fun saveText(file: File, content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun readText(file: File): String {
        return file.readText()
    }

    fun delete(file: File): Boolean {
        return file.delete()
    }
}
