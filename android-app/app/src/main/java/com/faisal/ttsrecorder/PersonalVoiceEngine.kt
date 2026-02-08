package com.faisal.ttsrecorder

import android.content.Context
import android.media.MediaPlayer
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File

class PersonalVoiceEngine(private val context: Context) {
    private var tts: OfflineTts? = null
    private var player: MediaPlayer? = null

    fun isModelInstalled(): Boolean {
        return try {
            val files = context.assets.list("voice") ?: return false
            files.contains("model.onnx") && files.contains("tokens.txt")
        } catch (_: Exception) {
            false
        }
    }

    fun speak(text: String, onDone: (String) -> Unit) {
        if (!isModelInstalled()) {
            onDone("Model files missing in assets/voice.")
            return
        }

        Thread {
            try {
                ensureTts()
                val audio = tts!!.generate(text = text, sid = 0, speed = 1.0f)
                if (audio.samples.isEmpty()) {
                    onDone("Synthesis failed: empty audio.")
                    return@Thread
                }

                val out = File(context.filesDir, "personal_voice.wav")
                audio.save(out.absolutePath)

                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(out.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        onDone("Done.")
                    }
                }
            } catch (e: Exception) {
                onDone("Synthesis failed: ${e.message}")
            }
        }.start()
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun ensureTts() {
        if (tts != null) return

        val modelConfig = OfflineTtsModelConfig(
            vits = OfflineTtsVitsModelConfig(
                model = "voice/model.onnx",
                tokens = "voice/tokens.txt",
                lexicon = "",
                dataDir = ""
            ),
            numThreads = 2,
            debug = false,
            provider = "cpu"
        )
        val config = OfflineTtsConfig(model = modelConfig)
        tts = OfflineTts(context.assets, config)
    }
}
