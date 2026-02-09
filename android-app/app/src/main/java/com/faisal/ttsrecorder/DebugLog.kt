package com.faisal.ttsrecorder

import android.content.Context
import android.os.Build
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val LOG_FILE_NAME = "tts_debug.log"
    private val lock = Any()
    private var appContext: Context? = null
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun appStartup() {
        val pageSize = try {
            Os.sysconf(OsConstants._SC_PAGESIZE)
        } catch (_: Exception) {
            -1L
        }
        i(
            "APP",
            "startup sdk=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER}/${Build.MODEL} abi=${Build.SUPPORTED_ABIS.joinToString()} pageSize=$pageSize"
        )
    }

    fun i(tag: String, msg: String) {
        append("I", tag, msg, null)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        append("E", tag, msg, tr)
    }

    fun readAll(): String {
        val file = getLogFile() ?: return "Log file unavailable."
        return if (file.exists()) file.readText() else "No logs yet."
    }

    fun clear() {
        val file = getLogFile() ?: return
        synchronized(lock) {
            if (file.exists()) {
                file.writeText("")
            }
        }
    }

    fun filePath(): String {
        return getLogFile()?.absolutePath ?: "unavailable"
    }

    fun buildReport(): String {
        val now = timeFormat.format(Date())
        return buildString {
            append("TTS Debug Report\n")
            append("Generated: ").append(now).append("\n")
            append("SDK: ").append(Build.VERSION.SDK_INT).append("\n")
            append("Device: ").append(Build.MANUFACTURER).append("/").append(Build.MODEL).append("\n")
            append("ABI: ").append(Build.SUPPORTED_ABIS.joinToString()).append("\n")
            append("Log file: ").append(filePath()).append("\n\n")
            append(readAll())
        }
    }

    private fun append(level: String, tag: String, msg: String, tr: Throwable?) {
        val line = buildString {
            append(timeFormat.format(Date()))
            append(" ")
            append(level)
            append("/")
            append(tag)
            append(": ")
            append(msg)
            if (tr != null) {
                append("\n")
                append(tr.stackTraceToString())
            }
            append("\n")
        }

        val file = getLogFile() ?: return
        synchronized(lock) {
            FileWriter(file, true).use { it.write(line) }
        }
    }

    private fun getLogFile(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, LOG_FILE_NAME)
    }
}
