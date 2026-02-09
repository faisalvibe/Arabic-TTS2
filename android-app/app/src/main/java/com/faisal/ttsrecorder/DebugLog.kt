package com.faisal.ttsrecorder

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.system.Os
import android.system.OsConstants
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
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
        val processName = if (Build.VERSION.SDK_INT >= 28) {
            try {
                Application.getProcessName()
            } catch (_: Exception) {
                "unknown"
            }
        } else {
            "unknown"
        }
        i(
            "APP",
            "startup sdk=${Build.VERSION.SDK_INT} process=$processName device=${Build.MANUFACTURER}/${Build.MODEL} abi=${Build.SUPPORTED_ABIS.joinToString()} pageSize=$pageSize"
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

    fun logRecentProcessExit() {
        if (Build.VERSION.SDK_INT < 30) return
        val ctx = appContext ?: return
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val processName = Application.getProcessName()
            val exits = am.getHistoricalProcessExitReasons(ctx.packageName, processName, 1)
            val info = exits.firstOrNull() ?: return

            val reasonName = reasonToString(info.reason)
            i(
                "APP",
                "last_exit process=$processName reason=$reasonName status=${info.status} importance=${info.importance} timestamp=${info.timestamp} desc=${info.description ?: "none"}"
            )

            val trace = readTraceSnippet(info, maxLines = 40, maxChars = 3000)
            if (trace.isNotEmpty()) {
                i("APP", "last_exit_trace\n$trace")
            }
        } catch (e: Exception) {
            e("APP", "last_exit_read_failed msg=${e.message}", e)
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

    private fun reasonToString(reason: Int): String {
        if (Build.VERSION.SDK_INT < 30) return reason.toString()
        return when (reason) {
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_CRASH -> "CRASH"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
            ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
            ApplicationExitInfo.REASON_OTHER -> "OTHER"
            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
            ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
            ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
            ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
            ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
            else -> reason.toString()
        }
    }

    private fun readTraceSnippet(info: ApplicationExitInfo, maxLines: Int, maxChars: Int): String {
        val input = info.traceInputStream ?: return ""
        return try {
            BufferedReader(InputStreamReader(input)).use { reader ->
                val out = StringBuilder()
                var lines = 0
                while (lines < maxLines && out.length < maxChars) {
                    val line = reader.readLine() ?: break
                    out.append(line).append('\n')
                    lines++
                }
                out.toString().trim()
            }
        } catch (_: Exception) {
            ""
        }
    }
}
