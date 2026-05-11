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
                    tts?.language = Locale.US
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
            RobotStatus.MOVING -> "I am moving to the destination."
            RobotStatus.AVOIDING -> "Obstacle detected. Adjusting my path."
            RobotStatus.BLOCKED -> "I am blocked. Please clear the way."
            RobotStatus.SLEEPING -> "I am going to sleep now."
            RobotStatus.IDLE -> "I am ready and waiting."
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
