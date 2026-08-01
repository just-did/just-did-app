package com.zhouyp.justdid.data.local.file

import java.io.File
import java.io.RandomAccessFile
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

    fun appendText(file: File, content: String) {
        file.parentFile?.mkdirs()
        file.appendText(content)
    }

    fun readLastNLines(file: File, n: Int): List<String> {
        if (!file.exists() || file.length() == 0L) return emptyList()

        val raf = RandomAccessFile(file, "r")
        val lines = mutableListOf<String>()
        val buf = StringBuilder()
        var pos = raf.length() - 1

        while (pos >= 0 && lines.size < n) {
            raf.seek(pos)
            val ch = raf.readByte().toInt().toChar()
            if (ch == '\n') {
                lines.add(buf.reverse().toString())
                buf.clear()
            } else {
                buf.append(ch)
            }
            pos--
        }

        if (buf.isNotEmpty()) {
            lines.add(buf.reverse().toString())
        }

        raf.close()

        // 移除尾部的空行（文件末尾换行符产生的空串）
        return lines.asReversed().dropLastWhile { it.isEmpty() }
    }
}
