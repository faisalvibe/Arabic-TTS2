package com.faisal.ttsrecorder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var textInput: EditText
    private lateinit var statusText: TextView
    private lateinit var personalVoiceEngine: PersonalVoiceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DebugLog.init(this)
        DebugLog.i("MAIN", "onCreate")

        textInput = findViewById(R.id.textInput)
        statusText = findViewById(R.id.statusText)
        personalVoiceEngine = PersonalVoiceEngine(this)
        statusText.text = if (
            personalVoiceEngine.isModelInstalled(PersonalVoiceEngine.VoiceLang.EN) ||
            personalVoiceEngine.isModelInstalled(PersonalVoiceEngine.VoiceLang.AR)
        ) {
            getString(R.string.tts_ready)
        } else {
            getString(R.string.tts_not_ready)
        }

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
            if (!personalVoiceEngine.isModelInstalled(lang)) {
                statusText.text = getString(
                    if (arSelected) R.string.custom_voice_missing_ar else R.string.custom_voice_missing_en
                )
                DebugLog.i("MAIN", "model_missing lang=$lang")
                return@setOnClickListener
            }

            statusText.text = getString(R.string.speaking_status)
            personalVoiceEngine.speak(text, lang) { result ->
                DebugLog.i("MAIN", "speak_result lang=$lang result=$result")
                runOnUiThread {
                    statusText.text = result
                }
            }
        }

        stopButton.setOnClickListener {
            DebugLog.i("MAIN", "stop_clicked")
            personalVoiceEngine.stop()
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
        personalVoiceEngine.stop()
        super.onDestroy()
    }
}
