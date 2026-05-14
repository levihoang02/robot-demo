package com.example.robotdemo.qna

import android.content.Context
import com.example.robotdemo.qna.llm.OpenAIProvider
import com.example.robotdemo.qna.stt.AndroidSTTProvider
import com.example.robotdemo.qna.tts.AndroidTTSProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object QnARepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var assistantManager: VoiceAssistantManager

    fun initialize(context: Context) {
        val stt = AndroidSTTProvider(context)
        val tts = AndroidTTSProvider(context)
        val llm = OpenAIProvider()
        
        assistantManager = VoiceAssistantManager(stt, llm, tts)
        
        scope.launch {
            assistantManager.start()
        }
    }

    val voiceState: StateFlow<VoiceState>
        get() = assistantManager.voiceState

    fun startListening() {
        scope.launch {
            assistantManager.startListening()
        }
    }

    fun stopListening() {
        scope.launch {
            assistantManager.stopListening()
        }
    }

    fun closeAssistant() {
        assistantManager.closeAssistant()
    }
}
