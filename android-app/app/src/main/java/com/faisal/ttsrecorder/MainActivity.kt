package com.faisal.ttsrecorder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var textInput: EditText
    private lateinit var statusText: TextView
    private var serviceMessenger: Messenger? = null
    private var isBound = false
    private val incomingMessenger = Messenger(
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == TtsEngineService.MSG_RESULT) {
                    val result = msg.data.getString(TtsEngineService.KEY_RESULT, "Done.")
                    DebugLog.i("MAIN", "ipc_result result=$result")
                    statusText.text = result
                } else {
                    super.handleMessage(msg)
                }
            }
        }
    )
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceMessenger = Messenger(service)
            isBound = true
            DebugLog.i("MAIN", "service_connected")
            statusText.text = getString(R.string.tts_ready)
            sendToService(TtsEngineService.MSG_PING, Bundle())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            serviceMessenger = null
            DebugLog.i("MAIN", "service_disconnected")
            statusText.text = getString(R.string.engine_crashed_status)
            bindEngineService()
        }

        override fun onBindingDied(name: ComponentName?) {
            isBound = false
            serviceMessenger = null
            DebugLog.i("MAIN", "service_binding_died")
            statusText.text = getString(R.string.engine_crashed_status)
            bindEngineService()
        }

        override fun onNullBinding(name: ComponentName?) {
            isBound = false
            serviceMessenger = null
            DebugLog.i("MAIN", "service_null_binding")
            statusText.text = getString(R.string.engine_unavailable_status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DebugLog.init(this)
        DebugLog.i("MAIN", "onCreate")

        textInput = findViewById(R.id.textInput)
        statusText = findViewById(R.id.statusText)
        statusText.text = getString(R.string.tts_loading)
        bindEngineService()

        val speakButton = findViewById<Button>(R.id.speakButton)
        val stopButton = findViewById<Button>(R.id.stopButton)
        val copyLogsButton = findViewById<Button>(R.id.copyLogsButton)
        val clearLogsButton = findViewById<Button>(R.id.clearLogsButton)

        speakButton.setOnClickListener {
            val text = textInput.text.toString().trim()
            if (text.isEmpty()) {
                statusText.text = getString(R.string.enter_text_hint)
                DebugLog.i("MAIN", "speak_clicked_empty_text")
                return@setOnClickListener
            }

            val arSelected = findViewById<RadioButton>(R.id.langAr).isChecked
            val lang = if (arSelected) PersonalVoiceEngine.VoiceLang.AR else PersonalVoiceEngine.VoiceLang.EN
            DebugLog.i("MAIN", "speak_clicked lang=$lang text_len=${text.length}")
            if (!isModelInstalledLocal(lang)) {
                statusText.text = getString(
                    if (arSelected) R.string.custom_voice_missing_ar else R.string.custom_voice_missing_en
                )
                DebugLog.i("MAIN", "model_missing lang=$lang")
                return@setOnClickListener
            }
            if (!isBound) {
                statusText.text = getString(R.string.engine_unavailable_status)
                DebugLog.i("MAIN", "speak_rejected_engine_unavailable")
                bindEngineService()
                return@setOnClickListener
            }

            statusText.text = getString(R.string.speaking_status)
            val data = Bundle().apply {
                putString(TtsEngineService.KEY_TEXT, text)
                putString(TtsEngineService.KEY_LANG, lang.name)
            }
            sendToService(TtsEngineService.MSG_SPEAK, data)
        }

        stopButton.setOnClickListener {
            DebugLog.i("MAIN", "stop_clicked")
            sendToService(TtsEngineService.MSG_STOP, Bundle())
            statusText.text = getString(R.string.stopped_status)
        }

        copyLogsButton.setOnClickListener {
            val logs = DebugLog.readAll()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("tts_debug_log", logs))
            statusText.text = getString(R.string.logs_copied_status, DebugLog.filePath())
            DebugLog.i("MAIN", "logs_copied")
        }

        clearLogsButton.setOnClickListener {
            DebugLog.clear()
            DebugLog.i("MAIN", "logs_cleared")
            statusText.text = getString(R.string.logs_cleared_status)
        }
    }

    override fun onDestroy() {
        DebugLog.i("MAIN", "onDestroy")
        if (isBound) {
            unbindService(connection)
            isBound = false
            serviceMessenger = null
        }
        super.onDestroy()
    }

    private fun bindEngineService() {
        val intent = Intent(this, TtsEngineService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        DebugLog.i("MAIN", "bind_engine_service")
    }

    private fun sendToService(what: Int, data: Bundle) {
        val target = serviceMessenger ?: return
        try {
            val msg = Message.obtain(null, what).apply {
                this.data = data
                replyTo = incomingMessenger
            }
            target.send(msg)
        } catch (e: Exception) {
            DebugLog.e("MAIN", "send_ipc_failed what=$what msg=${e.message}", e)
            statusText.text = getString(R.string.engine_unavailable_status)
            isBound = false
            serviceMessenger = null
            Handler(Looper.getMainLooper()).post { bindEngineService() }
        }
    }

    private fun isModelInstalledLocal(lang: PersonalVoiceEngine.VoiceLang): Boolean {
        val dir = if (lang == PersonalVoiceEngine.VoiceLang.EN) "voice/en" else "voice/ar"
        return try {
            val files = assets.list(dir) ?: return false
            files.contains("model.onnx") && files.contains("tokens.txt")
        } catch (_: Exception) {
            false
        }
    }
}
