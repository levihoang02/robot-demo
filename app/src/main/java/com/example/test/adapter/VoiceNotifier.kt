package com.example.test.adapter

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.test.domain.RobotStatus
import java.util.Locale

class VoiceNotifier(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private var lastNotifyTime = 0L
    private var lastStatus: RobotStatus? = null
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

        val now = System.currentTimeMillis()
        if (status == lastStatus && (now - lastNotifyTime) < NOTIFY_COOLDOWN_MS) {
            return
        }
        
        val message = when (status) {
            RobotStatus.MOVING -> "Tôi đang di chuyển đến điểm đích."
            RobotStatus.AVOIDING -> "Phát hiện vật cản. Đang điều chỉnh đường đi."
            RobotStatus.BLOCKED -> "Đường đi bị chặn. Vui lòng dọn dẹp lối đi."
            RobotStatus.IDLE -> "Tôi đã sẵn sàng và đang chờ lệnh."
            RobotStatus.GOAL_REACHED -> "Đã đến điểm đích."
            else -> return
        }
        
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        lastNotifyTime = now
        lastStatus = status
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
