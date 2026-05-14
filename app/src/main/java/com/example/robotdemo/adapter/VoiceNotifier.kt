package com.example.robotdemo.adapter

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.robotdemo.domain.RobotStatus
import java.util.Locale

class VoiceNotifier(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // Set language to Vietnamese
                    val result = tts?.setLanguage(Locale("vi", "VN"))
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
        
        val message = when (status) {
            RobotStatus.MOVING -> "Tôi đang di chuyển đến điểm đích."
            RobotStatus.AVOIDING -> "Phát hiện vật cản. Đang điều chỉnh đường đi."
            RobotStatus.BLOCKED -> "Đường đi bị chặn. Vui lòng dọn dẹp lối đi."
            RobotStatus.SLEEPING -> "Tôi đang đi ngủ đây."
            RobotStatus.IDLE -> "Tôi đã sẵn sàng và đang chờ lệnh."
            RobotStatus.GOAL_REACHED -> "Đã đến điểm đích."
        }
        
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
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
