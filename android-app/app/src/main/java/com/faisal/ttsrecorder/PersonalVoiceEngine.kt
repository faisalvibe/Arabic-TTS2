package com.faisal.ttsrecorder

import android.content.Context

class PersonalVoiceEngine(private val context: Context) {
    fun isModelInstalled(): Boolean {
        return try {
            val files = context.assets.list("voice") ?: return false
            files.contains("model.onnx") && files.contains("tokens.txt")
        } catch (_: Exception) {
            false
        }
    }
}
