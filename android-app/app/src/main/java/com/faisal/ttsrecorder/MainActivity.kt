package com.faisal.ttsrecorder

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var textInput: EditText
    private lateinit var statusText: TextView
    private lateinit var personalVoiceEngine: PersonalVoiceEngine
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textInput = findViewById(R.id.textInput)
        statusText = findViewById(R.id.statusText)
        personalVoiceEngine = PersonalVoiceEngine(this)
        tts = TextToSpeech(this, this)

        val speakButton = findViewById<Button>(R.id.speakButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        speakButton.setOnClickListener {
            if (!ready) {
                statusText.text = getString(R.string.tts_not_ready)
                return@setOnClickListener
            }
            val text = textInput.text.toString().trim()
            if (text.isEmpty()) {
                statusText.text = getString(R.string.enter_text_hint)
                return@setOnClickListener
            }

            val customMode = findViewById<RadioButton>(R.id.modeCustom).isChecked
            if (customMode) {
                if (!personalVoiceEngine.isModelInstalled()) {
                    statusText.text = getString(R.string.custom_voice_missing)
                    return@setOnClickListener
                }
                statusText.text = getString(R.string.custom_voice_missing)
                return@setOnClickListener
            }

            val arSelected = findViewById<RadioButton>(R.id.langAr).isChecked
            val locale = if (arSelected) Locale("ar") else Locale.ENGLISH
            val setLang = tts.setLanguage(locale)
            if (setLang == TextToSpeech.LANG_MISSING_DATA || setLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                statusText.text = getString(R.string.language_not_supported)
                return@setOnClickListener
            }

            val utteranceId = "tts-${System.currentTimeMillis()}"
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            statusText.text = getString(R.string.speaking_status)
        }

        stopButton.setOnClickListener {
            if (ready) {
                tts.stop()
            }
            statusText.text = getString(R.string.stopped_status)
        }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        statusText.text = if (ready) {
            getString(R.string.tts_ready)
        } else {
            getString(R.string.tts_not_ready)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
