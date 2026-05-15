package com.example.test.adapter

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.test.domain.RobotStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceNotifier(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private var lastNotifyTime = 0L
    private var lastStatus: RobotStatus? = null

    private var speakJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    companion object {
        private const val NOTIFY_COOLDOWN_MS = 3000L
    }

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // Set language to Vietnamese
                    val result = tts?.setLanguage(
                        Locale.forLanguageTag("vi-VN")
                    )
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fallback to US English if Vietnamese is not installed on the tablet
                        tts?.language = Locale.US
                    }
                    isReady = true
                }
            }
        } catch (e: Exception) {
            // TextToSpeech might not be available in all environments (like Previews)
            e.printStackTrace()
        }
    }

    fun notifyStatus(status: RobotStatus) {

        val tts = this.tts ?: return
        if (!isReady) return

        // 1. filter low priority spam
        if (!shouldSpeak(status)) return

        speakJob?.cancel()

        speakJob = scope.launch {

            delay(500)

            // confirm state still same
            if (status != lastStatus) return@launch

            val now = System.currentTimeMillis()

            if (now - lastNotifyTime < NOTIFY_COOLDOWN_MS) return@launch

            val message = getMessage(status)

            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)

            lastNotifyTime = now
            lastStatus = status
        }
    }

    private fun shouldSpeak(status: RobotStatus): Boolean {
        return when (status) {
            RobotStatus.BLOCKED -> true
            RobotStatus.GOAL_REACHED -> true
            RobotStatus.AVOIDING -> true
            RobotStatus.MOVING -> false
            RobotStatus.IDLE -> true
            else -> false
        }
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
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
