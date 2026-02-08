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
            val hasModel = files.contains("model.onnx")
            val hasTokens = files.contains("tokens.txt")
            val hasJson = files.contains("model.onnx.json")
            val ok = hasModel && hasTokens
            DebugLog.i(
                "ENGINE",
                "isModelInstalled lang=$lang dir=$dir ok=$ok hasModel=$hasModel hasTokens=$hasTokens hasJson=$hasJson files=${files.joinToString()}"
            )
            ok
        } catch (_: Exception) {
            DebugLog.i("ENGINE", "isModelInstalled lang=$lang dir=$dir failed_to_list_assets")
            false
        }
    }

    fun speak(text: String, lang: VoiceLang, onDone: (String) -> Unit) {
        if (isSpeaking) {
            DebugLog.i("ENGINE", "speak_rejected_already_speaking lang=$lang")
            onDone("Already speaking. Tap Stop and try again.")
            return
        }
        if (!isModelInstalled(lang)) {
            DebugLog.i("ENGINE", "speak_rejected_model_missing lang=$lang")
            onDone("Model files missing in assets/${modelDir(lang)}.")
            return
        }

        Thread {
            isSpeaking = true
            DebugLog.i("ENGINE", "speak_start lang=$lang text_len=${text.length}")
            try {
                val tts = ensureTts(lang)
                DebugLog.i("ENGINE", "generate_start lang=$lang")
                val audio = tts.generate(text = text, sid = 0, speed = 1.0f)
                DebugLog.i("ENGINE", "generate_done lang=$lang samples=${audio.samples.size} sampleRate=${audio.sampleRate}")
                if (audio.samples.isEmpty()) {
                    isSpeaking = false
                    onDone("Synthesis failed: empty audio.")
                    return@Thread
                }

                val out = File(context.filesDir, "personal_voice_${lang.name.lowercase()}.wav")
                audio.save(out.absolutePath)
                DebugLog.i("ENGINE", "audio_saved path=${out.absolutePath} bytes=${out.length()}")

                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(out.absolutePath)
                    prepare()
                    start()
                    DebugLog.i("ENGINE", "playback_started lang=$lang")
                    setOnCompletionListener {
                        isSpeaking = false
                        DebugLog.i("ENGINE", "playback_completed lang=$lang")
                        onDone("Done.")
                    }
                    setOnErrorListener { _, _, _ ->
                        isSpeaking = false
                        DebugLog.i("ENGINE", "playback_error lang=$lang")
                        onDone("Playback failed.")
                        true
                    }
                }
            } catch (e: Exception) {
                isSpeaking = false
                DebugLog.e("ENGINE", "speak_exception lang=$lang msg=${e.message}", e)
                onDone("Synthesis failed: ${e.message}")
            }
        }.start()
    }

    fun stop() {
        DebugLog.i("ENGINE", "stop")
        player?.stop()
        player?.release()
        player = null
        isSpeaking = false
    }

    private fun ensureTts(lang: VoiceLang): OfflineTts {
        if (lang == VoiceLang.EN && ttsEn != null) return ttsEn!!
        if (lang == VoiceLang.AR && ttsAr != null) return ttsAr!!

        val dir = modelDir(lang)
        DebugLog.i("ENGINE", "ensureTts_create lang=$lang dir=$dir")
        val hasEspeakData = try {
            (context.assets.list(dir) ?: emptyArray()).contains("espeak-ng-data")
        } catch (_: Exception) {
            false
        }
        DebugLog.i("ENGINE", "ensureTts_espeak lang=$lang hasEspeakData=$hasEspeakData")

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
        DebugLog.i("ENGINE", "ensureTts_created lang=$lang")
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
