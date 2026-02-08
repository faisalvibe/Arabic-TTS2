package com.faisal.ttsrecorder

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException

class TtsEngineService : Service() {
    companion object {
        const val MSG_SPEAK = 1
        const val MSG_STOP = 2
        const val MSG_PING = 3

        const val MSG_RESULT = 100

        const val KEY_TEXT = "text"
        const val KEY_LANG = "lang"
        const val KEY_RESULT = "result"
    }

    private lateinit var engine: PersonalVoiceEngine
    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_PING -> {
                    reply(msg.replyTo, "pong")
                }

                MSG_STOP -> {
                    DebugLog.i("ENGINE-SVC", "msg_stop")
                    engine.stop()
                    reply(msg.replyTo, "Stopped.")
                }

                MSG_SPEAK -> {
                    val text = msg.data.getString(KEY_TEXT, "").trim()
                    val langRaw = msg.data.getString(KEY_LANG, "EN")
                    val lang = if (langRaw == "AR") {
                        PersonalVoiceEngine.VoiceLang.AR
                    } else {
                        PersonalVoiceEngine.VoiceLang.EN
                    }
                    DebugLog.i("ENGINE-SVC", "msg_speak lang=$lang text_len=${text.length}")
                    if (text.isEmpty()) {
                        reply(msg.replyTo, "Type text first.")
                        return
                    }
                    engine.speak(text, lang) { result ->
                        reply(msg.replyTo, result)
                    }
                }

                else -> super.handleMessage(msg)
            }
        }
    }
    private val messenger = Messenger(incomingHandler)

    override fun onCreate() {
        super.onCreate()
        DebugLog.init(this)
        DebugLog.i("ENGINE-SVC", "onCreate")
        engine = PersonalVoiceEngine(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder {
        DebugLog.i("ENGINE-SVC", "onBind")
        return messenger.binder
    }

    override fun onDestroy() {
        DebugLog.i("ENGINE-SVC", "onDestroy")
        engine.stop()
        super.onDestroy()
    }

    private fun reply(target: Messenger?, result: String) {
        if (target == null) return
        try {
            val data = Bundle().apply {
                putString(KEY_RESULT, result)
            }
            val out = Message.obtain(null, MSG_RESULT).apply { this.data = data }
            target.send(out)
        } catch (e: RemoteException) {
            DebugLog.e("ENGINE-SVC", "reply_failed msg=${e.message}", e)
        }
    }
}
