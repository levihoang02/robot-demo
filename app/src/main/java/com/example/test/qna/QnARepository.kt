package com.example.test.qna

import android.content.Context
import com.example.test.qna.llm.OpenAIProvider
import com.example.test.qna.llm.GeminiProvider
import com.example.test.qna.stt.AndroidSTTProvider
import com.example.test.qna.tts.AndroidTTSProvider
import com.example.test.qna.tts.ElevenLabsTTSProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object QnARepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var assistantManager: VoiceAssistantManager? = null
    
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    fun initialize(context: Context) {
        if (assistantManager != null) return
        
        val stt = AndroidSTTProvider(context)
        val tts = ElevenLabsTTSProvider(context, "", "hsndbHLHBSEcuDiMW1O9")
        val llm = GeminiProvider("")
        
        val manager = VoiceAssistantManager(stt, llm, tts)
        assistantManager = manager
        
        scope.launch {
            manager.start()
        }

        scope.launch {
            manager.voiceState.collect {
                _voiceState.value = it
            }
        }
    }

    fun startListening() {
        scope.launch {
            assistantManager?.startListening()
        }
    }

    fun stopListening() {
        scope.launch {
            assistantManager?.stopListening()
        }
    }

    fun closeAssistant() {
        scope.launch {
            assistantManager?.closeAssistant()
        }
    }
}
