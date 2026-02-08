package com.faisal.ttsrecorder

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

        speakButton.setOnClickListener {
            val text = textInput.text.toString().trim()
            if (text.isEmpty()) {
                statusText.text = getString(R.string.enter_text_hint)
                return@setOnClickListener
            }

            val arSelected = findViewById<RadioButton>(R.id.langAr).isChecked
            val lang = if (arSelected) PersonalVoiceEngine.VoiceLang.AR else PersonalVoiceEngine.VoiceLang.EN
            if (!personalVoiceEngine.isModelInstalled(lang)) {
                statusText.text = getString(
                    if (arSelected) R.string.custom_voice_missing_ar else R.string.custom_voice_missing_en
                )
                return@setOnClickListener
            }

            statusText.text = getString(R.string.speaking_status)
            personalVoiceEngine.speak(text, lang) { result ->
                runOnUiThread {
                    statusText.text = result
                }
            }
        }

        stopButton.setOnClickListener {
            personalVoiceEngine.stop()
            statusText.text = getString(R.string.stopped_status)
        }
    }

    override fun onDestroy() {
        personalVoiceEngine.stop()
        super.onDestroy()
    }
}
