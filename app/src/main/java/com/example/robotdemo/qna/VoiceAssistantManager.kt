package com.example.robotdemo.qna

import com.example.robotdemo.qna.domain.VoiceRequest
import com.example.robotdemo.qna.llm.LLMProvider
import com.example.robotdemo.qna.stt.SpeechToTextProvider
import com.example.robotdemo.qna.tts.TextToSpeechProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

class VoiceAssistantManager(
    private val stt: SpeechToTextProvider,
    private val llm: LLMProvider,
    private val tts: TextToSpeechProvider
) {

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState = _voiceState.asStateFlow()

    suspend fun start() {
        stt.transcriptFlow.collectLatest { text ->
            if (text.isBlank()) return@collectLatest
            
            try {
                _voiceState.value = VoiceState.Processing

                val response = llm.generateReply(
                    VoiceRequest(text)
                )

                _voiceState.value = VoiceState.Speaking(response.text)
                tts.speak(response)

                // We stay in Speaking state until user closes or another process happens
                // For simplicity in this demo, we can return to Idle after some time or manual close
            } catch (e: Exception) {
                _voiceState.value = VoiceState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun startListening() {
        _voiceState.value = VoiceState.Listening
        stt.startListening()
    }

    suspend fun stopListening() {
        stt.stopListening()
        // If we were listening but didn't get results yet, go back to Idle
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.Idle
        }
    }

    fun closeAssistant() {
        tts.stop()
        _voiceState.value = VoiceState.Idle
    }
}
