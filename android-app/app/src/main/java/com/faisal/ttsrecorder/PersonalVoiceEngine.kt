package com.faisal.ttsrecorder

import android.content.Context
import android.media.MediaPlayer
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File

class PersonalVoiceEngine(private val context: Context) {
    enum class VoiceLang {
        EN,
        AR
    }

    private var ttsEn: OfflineTts? = null
    private var ttsAr: OfflineTts? = null
    private var player: MediaPlayer? = null
    @Volatile
    private var isSpeaking = false

    fun isModelInstalled(lang: VoiceLang): Boolean {
        val dir = modelDir(lang)
        return try {
            val files = context.assets.list(dir) ?: return false
            files.contains("model.onnx") && files.contains("tokens.txt")
        } catch (_: Exception) {
            false
        }
    }

    fun speak(text: String, lang: VoiceLang, onDone: (String) -> Unit) {
        if (isSpeaking) {
            onDone("Already speaking. Tap Stop and try again.")
            return
        }
        if (!isModelInstalled(lang)) {
            onDone("Model files missing in assets/${modelDir(lang)}.")
            return
        }

        Thread {
            isSpeaking = true
            try {
                val tts = ensureTts(lang)
                val audio = tts.generate(text = text, sid = 0, speed = 1.0f)
                if (audio.samples.isEmpty()) {
                    onDone("Synthesis failed: empty audio.")
                    return@Thread
                }

                val out = File(context.filesDir, "personal_voice_${lang.name.lowercase()}.wav")
                audio.save(out.absolutePath)

                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(out.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        isSpeaking = false
                        onDone("Done.")
                    }
                    setOnErrorListener { _, _, _ ->
                        isSpeaking = false
                        onDone("Playback failed.")
                        true
                    }
                }
            } catch (e: Exception) {
                isSpeaking = false
                onDone("Synthesis failed: ${e.message}")
            }
        }.start()
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
        isSpeaking = false
    }

    private fun ensureTts(lang: VoiceLang): OfflineTts {
        if (lang == VoiceLang.EN && ttsEn != null) return ttsEn!!
        if (lang == VoiceLang.AR && ttsAr != null) return ttsAr!!

        val dir = modelDir(lang)
        val hasEspeakData = try {
            (context.assets.list(dir) ?: emptyArray()).contains("espeak-ng-data")
        } catch (_: Exception) {
            false
        }

        val modelConfig = OfflineTtsModelConfig(
            vits = OfflineTtsVitsModelConfig(
                model = "$dir/model.onnx",
                tokens = "$dir/tokens.txt",
                lexicon = "",
                dataDir = if (hasEspeakData) "$dir/espeak-ng-data" else ""
            ),
            numThreads = 1,
            debug = false,
            provider = "cpu"
        )
        val config = OfflineTtsConfig(model = modelConfig)
        val instance = OfflineTts(context.assets, config)
        if (lang == VoiceLang.EN) {
            ttsEn = instance
        } else {
            ttsAr = instance
        }
        return instance
    }

    private fun modelDir(lang: VoiceLang): String {
        return if (lang == VoiceLang.EN) "voice/en" else "voice/ar"
    }
}
