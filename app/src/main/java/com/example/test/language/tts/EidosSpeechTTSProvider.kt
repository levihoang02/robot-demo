package com.example.test.language.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.test.qna.domain.VoiceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EidosSpeechTTSProvider(
    private val context: Context,
    private val apiKey: String
) : TextToSpeechProvider {

    companion object {
        private const val TAG = "AndroidSTT"

        // đổi voice nếu muốn
        private const val DEFAULT_VOICE = "vi-VN-HoaiMyNeural"
    }

    private val client = OkHttpClient()

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayerStarted = false

    override suspend fun speak(
        response: VoiceResponse
    ) = withContext(Dispatchers.IO) {

        Log.d(TAG, "EidosSpeech speak() start")

        stop()

        val json = JSONObject().apply {

            put("text", response.text)

            put("voice", DEFAULT_VOICE)

            put("format", "mp3")
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()

            // endpoint
            .url("https://eidosspeech.xyz/api/v1/tts")

            .addHeader(
                "X-API-Key",
                apiKey
            )

            .addHeader(
                "Content-Type",
                "application/json"
            )

            .addHeader(
                "Accept",
                "audio/mpeg"
            )

            .post(body)

            .build()

        Log.d(TAG, "Sending TTS request")

        val responseHttp = client
            .newCall(request)
            .execute()

        Log.d(
            TAG,
            "HTTP response=${responseHttp.code}"
        )

        if (!responseHttp.isSuccessful) {

            val errorBody = responseHttp.body?.string()

            Log.e(
                TAG,
                "EidosSpeech API error=$errorBody"
            )

            throw Exception(
                "EidosSpeech API error ${responseHttp.code}"
            )
        }

        val audioBytes = responseHttp.body?.bytes()
            ?: throw Exception("Audio bytes null")

        Log.d(
            TAG,
            "Audio bytes=${audioBytes.size}"
        )

        val audioFile = File.createTempFile(
            "eidosspeech_tts",
            ".mp3",
            context.cacheDir
        ).apply {
            writeBytes(audioBytes)
        }

        Log.d(
            TAG,
            "Audio saved=${audioFile.absolutePath}"
        )

        playAudio(audioFile)
    }

    private suspend fun playAudio(
        file: File
    ) = suspendCancellableCoroutine<Unit> { continuation ->

        Log.d(TAG, "playAudio()")

        try {

            mediaPlayer = MediaPlayer().apply {

                setDataSource(file.absolutePath)

                setOnPreparedListener {

                    Log.d(
                        TAG,
                        "MediaPlayer prepared"
                    )

                    isPlayerStarted = true

                    start()
                }

                setOnCompletionListener {

                    Log.d(
                        TAG,
                        "Playback completed"
                    )

                    isPlayerStarted = false

                    release()

                    mediaPlayer = null

                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                setOnErrorListener { _, what, extra ->

                    Log.e(
                        TAG,
                        "MediaPlayer error what=$what extra=$extra"
                    )

                    isPlayerStarted = false

                    release()

                    mediaPlayer = null

                    if (continuation.isActive) {

                        continuation.resumeWithException(
                            Exception(
                                "MediaPlayer error what=$what extra=$extra"
                            )
                        )
                    }

                    true
                }

                prepareAsync()
            }

            continuation.invokeOnCancellation {

                Log.d(
                    TAG,
                    "Coroutine cancelled"
                )

                stop()
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "playAudio exception",
                e
            )

            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    override fun stop() {

        Log.d(TAG, "stop()")

        try {

            val mp = mediaPlayer

            mediaPlayer = null

            if (mp != null) {

                if (isPlayerStarted) {

                    try {
                        mp.stop()
                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "mp.stop error",
                            e
                        )
                    }
                }

                mp.release()
            }

            isPlayerStarted = false

        } catch (e: Exception) {

            Log.e(
                TAG,
                "stop() error",
                e
            )
        }
    }

    override fun release() {

        Log.d(TAG, "release()")

        stop()
    }
}