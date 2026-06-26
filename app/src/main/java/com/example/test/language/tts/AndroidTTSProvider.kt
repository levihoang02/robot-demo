package com.example.test.language.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.test.qna.domain.VoiceResponse
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidTTSProvider(
    private val context: Context
) : TextToSpeechProvider {

    private var tts: TextToSpeech? = null

    private var isInitialized = false

    private suspend fun ensureInitialized(): Boolean = suspendCancellableCoroutine { continuation ->

        if (isInitialized) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        Log.d("AndroidTTS", "Initializing TTS...")
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("vi", "VN")
                isInitialized = true
                Log.d("AndroidTTS", "TTS Initialized successfully")
                if (continuation.isActive) continuation.resume(true)
            } else {
                Log.e("AndroidTTS", "TTS Initialization failed: $status")
                if (continuation.isActive) continuation.resume(false)
            }
        }
        
        continuation.invokeOnCancellation {
            // Initialization can't really be cancelled easily, but we can prevent double resume
        }
    }

    override suspend fun speak(
        response: VoiceResponse
    ) {
        if (!ensureInitialized()) return

        suspendCancellableCoroutine<Unit> { continuation ->
            val utteranceId = UUID.randomUUID().toString()
            
            val listener = object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        Log.e("AndroidTTS", "Error speaking utterance: $id")
                        continuation.resume(Unit)
                    }
                }
            }

            tts?.setOnUtteranceProgressListener(listener)
            
            continuation.invokeOnCancellation {
                Log.d("AndroidTTS", "Speaking cancelled, stopping TTS")
                tts?.stop()
            }

            val result = tts?.speak(
                response.text,
                TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                utteranceId
            )

            if (result == TextToSpeech.ERROR) {
                Log.e("AndroidTTS", "tts.speak returned ERROR")
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    override fun stop() {
        Log.d("AndroidTTS", "Stop called")
        tts?.stop()
    }

    override fun release() {
        Log.d("AndroidTTS", "Release called")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
