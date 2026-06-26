package com.example.test.adapter

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.test.domain.RobotStatus
import kotlinx.coroutines.*
import java.util.Locale

class VoiceNotifier(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    private var lastNotifyTime = 0L
    private var lastStatus: RobotStatus? = null

    private var speakJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val TAG = "VoiceNotifier"
        private const val NOTIFY_COOLDOWN_MS = 3000L
    }

    init {
        try {
            Log.d(TAG, "Init TTS...")

            tts = TextToSpeech(context) { status ->
                Log.d(TAG, "TTS init callback status=$status")

                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.forLanguageTag("vi-VN"))

                    Log.d(TAG, "Set language result=$result")

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        Log.w(TAG, "Vietnamese not supported, fallback to US")
                        tts?.language = Locale.US



                    }

                    isReady = true
                    Log.d(TAG, "TTS ready = true")
                } else {
                    Log.e(TAG, "TTS init failed status=$status")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "TTS exception", e)
        }
    }

    fun notifyStatus(status: RobotStatus) {

        Log.d(TAG, "notifyStatus: $status")

        val tts = this.tts ?: return
        if (!isReady) return

        if (!shouldSpeak(status)) return

        val now = System.currentTimeMillis()

        speakJob?.cancel()

        speakJob = scope.launch {

            delay(500)

            // ❌ chỉ check cooldown, KHÔNG check status khác
            if (System.currentTimeMillis() - lastNotifyTime < NOTIFY_COOLDOWN_MS) {
                Log.d(TAG, "Cooldown skip: $status")
                return@launch
            }

            val message = getMessage(status)

            if (message.isBlank()) {
                Log.w(TAG, "Empty message for $status")
                return@launch
            }

            Log.d(TAG, "SPEAK -> $status : $message")

            tts.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "voice_${status.name}"
            )

            lastStatus = status
            lastNotifyTime = System.currentTimeMillis()
        }
    }

    private fun shouldSpeak(status: RobotStatus): Boolean {
        val result = when (status) {
            RobotStatus.BLOCKED -> true
            RobotStatus.GOAL_REACHED -> true
            RobotStatus.AVOIDING -> true
            RobotStatus.MOVING -> true
            RobotStatus.IDLE -> true
            else -> false
        }

        Log.d(TAG, "shouldSpeak($status) = $result")
        return result
    }

    private fun getMessage(status: RobotStatus): String {
        return when (status) {
            RobotStatus.MOVING -> "Tôi đang di chuyển đến điểm đích."
            RobotStatus.AVOIDING -> "Phát hiện vật cản. Đang điều chỉnh đường đi."
            RobotStatus.BLOCKED -> "Đường đi bị chặn. Vui lòng dọn dẹp lối đi."
            RobotStatus.IDLE -> "Tôi đã sẵn sàng và đang chờ lệnh."
            RobotStatus.GOAL_REACHED -> "Đã đến điểm đích."
            else -> ""
        }
    }

    fun shutdown() {
        Log.d(TAG, "shutdown called")

        try {
            speakJob?.cancel()
            tts?.stop()
            tts?.shutdown()
            Log.d(TAG, "TTS shutdown complete")
        } catch (e: Exception) {
            Log.e(TAG, "shutdown error", e)
        }
    }
}