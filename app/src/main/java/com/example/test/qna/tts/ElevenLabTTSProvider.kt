package com.example.test.qna.tts

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

class ElevenLabsTTSProvider(
    private val context: Context,
    private val apiKey: String,
    private val voiceId: String
) : TextToSpeechProvider {

    companion object {
        private const val TAG = "AndroidSTT"
    }

    private val client = OkHttpClient()

    private var mediaPlayer: MediaPlayer? = null

    override suspend fun speak(
        response: VoiceResponse
    ) = withContext(Dispatchers.IO) {

        Log.d(TAG, "TTS speak() start text=${response.text}")

        stop()

        val json = JSONObject().apply {
            put("text", response.text)
            put(
                "voice_settings",
                JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                }
            )
        }

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            .addHeader("xi-api-key", apiKey)
            .addHeader("Accept", "audio/mpeg")
            .post(
                json.toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .build()

        Log.d(TAG, "Request URL: ${request.url}")
        Log.d(TAG, "VoiceID: $voiceId")

        val responseHttp = client.newCall(request).execute()

        Log.d(TAG, "HTTP response code: ${responseHttp.code}")

        if (!responseHttp.isSuccessful) {

            val errorBody = responseHttp.body?.string()

            Log.e(TAG, "API ERROR code=${responseHttp.code} body=$errorBody")

            throw Exception("ElevenLabs API Error: ${responseHttp.code}")
        }

        val audioBytes = responseHttp.body?.bytes()
            ?: run {
                Log.e(TAG, "Audio response is null")
                throw Exception("Audio response is null")
            }

        Log.d(TAG, "Audio bytes size=${audioBytes.size}")

        val audioFile = File.createTempFile(
            "tts_audio",
            ".mp3",
            context.cacheDir
        ).apply {
            writeBytes(audioBytes)
        }

        Log.d(TAG, "Audio file saved: ${audioFile.absolutePath}")

        playAudio(audioFile)
    }

    private suspend fun playAudio(
        file: File
    ) = suspendCancellableCoroutine<Unit> { continuation ->

        Log.d(TAG, "playAudio() file=${file.absolutePath}")

        try {

            mediaPlayer = MediaPlayer().apply {

                setDataSource(file.absolutePath)

                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared -> start()")
                    start()
                }

                setOnCompletionListener {

                    Log.d(TAG, "MediaPlayer completed")

                    release()
                    mediaPlayer = null

                    continuation.resume(Unit)
                }

                setOnErrorListener { _, what, extra ->

                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")

                    release()
                    mediaPlayer = null

                    continuation.resumeWithException(
                        Exception("MediaPlayer error what=$what extra=$extra")
                    )

                    true
                }

                prepareAsync()
            }

            continuation.invokeOnCancellation {
                Log.d(TAG, "Coroutine cancelled -> stop()")
                stop()
            }

        } catch (e: Exception) {

            Log.e(TAG, "playAudio exception", e)
            continuation.resumeWithException(e)
        }
    }

    override fun stop() {

        Log.d(TAG, "stop() called")

        try {

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

        } catch (e: Exception) {

            Log.e(TAG, "stop error", e)
        }
    }

    override fun release() {

        Log.d(TAG, "release() called")
        stop()
    }
}