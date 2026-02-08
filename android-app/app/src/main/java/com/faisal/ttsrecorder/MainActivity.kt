package com.faisal.ttsrecorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urlInput = findViewById<EditText>(R.id.urlInput)
        val openButton = findViewById<Button>(R.id.openButton)

        openButton.setOnClickListener {
            var url = urlInput.text.toString().trim()
            if (url.isEmpty()) {
                url = "https://tts-recorder2.vercel.app/tts-recorder"
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
