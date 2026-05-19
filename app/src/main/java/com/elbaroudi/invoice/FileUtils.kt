package com.elbaroudi.invoice


import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun getAppDownloadsDir(): File {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "InvoiceApp"
        )
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }

    fun createSubDirectory(parentDir: File, dirName: String): File {
        val subDir = File(parentDir, dirName)
        if (!subDir.exists()) {
            subDir.mkdirs()
        }
        return subDir
    }

    fun generateUniqueFileName(baseName: String, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${baseName}_$timestamp.$extension"
    }

    fun isValidFileName(fileName: String): Boolean {
        return fileName.isNotBlank() && !fileName.contains(Regex("[<>:\"/\\|?*]"))
    }

    fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[<>:\"/\\|?*]"), "_")
    }
}