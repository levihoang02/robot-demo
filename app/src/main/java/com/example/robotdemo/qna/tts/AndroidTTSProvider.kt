package com.example.robotdemo.qna.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.robotdemo.qna.domain.VoiceResponse
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidTTSProvider(
    private val context: Context
) : TextToSpeechProvider {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private suspend fun ensureInitialized(): Boolean = suspendCoroutine { continuation ->
        if (isInitialized) {
            continuation.resume(true)
            return@suspendCoroutine
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("vi", "VN")
                isInitialized = true
                continuation.resume(true)
            } else {
                continuation.resume(false)
            }
        }
    }

    override suspend fun speak(response: VoiceResponse) {
        if (!ensureInitialized()) return

        tts?.speak(response.text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun stop() {
        tts?.stop()
    }
}
