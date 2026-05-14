package com.example.test.qna.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.test.qna.domain.VoiceResponse
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidTTSProvider(
    private val context: Context
) : TextToSpeechProvider {

    private var tts: TextToSpeech? = null

    private var isInitialized = false

    private suspend fun ensureInitialized():
            Boolean = suspendCoroutine { continuation ->

        if (isInitialized) {

            continuation.resume(true)

            return@suspendCoroutine
        }

        tts = TextToSpeech(context) { status ->

            if (status == TextToSpeech.SUCCESS) {

                tts?.language =
                    Locale("vi", "VN")

                isInitialized = true

                continuation.resume(true)

            } else {

                continuation.resume(false)
            }
        }
    }

    override suspend fun speak(
        response: VoiceResponse
    ) {

        if (!ensureInitialized()) return

        suspendCoroutine<Unit> { continuation ->

            val utteranceId =
                UUID.randomUUID().toString()

            tts?.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {

                    override fun onStart(
                        utteranceId: String?
                    ) {}

                    override fun onDone(
                        utteranceId: String?
                    ) {

                        continuation.resume(Unit)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(
                        utteranceId: String?
                    ) {

                        continuation.resume(Unit)
                    }
                }
            )

            tts?.speak(
                response.text,
                TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                utteranceId
            )
        }
    }

    override fun stop() {

        tts?.stop()
    }

    override fun release() {

        tts?.stop()

        tts?.shutdown()

        tts = null

        isInitialized = false
    }
}