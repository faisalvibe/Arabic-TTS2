package com.faisal.ttsrecorder

import android.app.Application

class TtsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugLog.init(this)
        DebugLog.appStartup()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            DebugLog.e("CRASH", "Uncaught exception on thread=${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
