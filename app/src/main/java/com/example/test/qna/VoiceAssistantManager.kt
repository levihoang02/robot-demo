package com.example.test.qna

import android.util.Log
import com.example.test.qna.domain.VoiceRequest
import com.example.test.qna.llm.LLMProvider
import com.example.test.qna.stt.SpeechToTextProvider
import com.example.test.qna.stt.STTEvent
import com.example.test.qna.tts.TextToSpeechProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

class VoiceAssistantManager(
    private val stt: SpeechToTextProvider,
    private val llm: LLMProvider,
    private val tts: TextToSpeechProvider
) {

    companion object {

        private const val TAG =
            "VoiceAssistantManager"
    }

    private val _voiceState =
        MutableStateFlow<VoiceState>(
            VoiceState.Idle
        )

    val voiceState =
        _voiceState.asStateFlow()

    suspend fun start() {

        while (true) {
            try {
                stt.transcriptFlow.collectLatest { event ->
                    handleSTTEvent(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcript flow collection failed, retrying...", e)
                delay(1000)
            }
        }
    }

    private suspend fun handleSTTEvent(event: STTEvent) {
        if (_voiceState.value == VoiceState.Idle) {
            Log.d(TAG, "Assistant is Idle, ignoring STT event: $event")
            return
        }

        when (event) {
            /**
             * User speech recognized
             */
            is STTEvent.Transcript -> {

                if (event.text.isBlank()) {
                    if (_voiceState.value != VoiceState.Idle) {
                        _voiceState.value = VoiceState.WaitingForRetry
                    }
                    return
                }

                try {
                    _voiceState.value = VoiceState.Processing
                    val response = llm.generateReply(VoiceRequest(event.text))
                    
                    if (_voiceState.value == VoiceState.Idle) {
                        Log.d(TAG, "Assistant closed while processing LLM")
                        return
                    }

                    _voiceState.value = VoiceState.Speaking(response.text)
                    tts.speak(response)

                    if (_voiceState.value == VoiceState.Idle) {
                        Log.d(TAG, "Assistant closed while speaking")
                        return
                    }
                    
                    _voiceState.value = VoiceState.WaitingForRetry

                } catch (e: Exception) {
                    if (_voiceState.value == VoiceState.Idle) return
                    Log.e(TAG, "LLM/TTS Error", e)
                    _voiceState.value = VoiceState.Error(e.message ?: "Unknown error")
                }
            }

            /**
             * No voice detected
             * after timeout
             */
            STTEvent.SilenceTimeout -> {
                if (_voiceState.value != VoiceState.Idle) {
                    _voiceState.value = VoiceState.WaitingForRetry
                }
            }

            /**
             * STT error
             */
            is STTEvent.Error -> {
                if (_voiceState.value != VoiceState.Idle) {
                    Log.e(TAG, "STT Error: ${event.message}")
                    _voiceState.value = VoiceState.Error(
                        buildString {
                            appendLine("STT Error")
                            appendLine()
                            appendLine("Code: ${event.code}")
                            appendLine("Message: ${event.message}")
                        }
                    )
                }
            }
        }
    }

    suspend fun startListening() {

        _voiceState.value =
            VoiceState.Listening

        stt.startListening()
    }

    suspend fun stopListening() {

        stt.stopListening()

        if (
            _voiceState.value
                    is VoiceState.Listening
        ) {

            _voiceState.value =
                VoiceState.WaitingForRetry
        }
    }

    suspend fun closeAssistant() {

        stt.stopListening()

        tts.stop()

        _voiceState.value =
            VoiceState.Idle
    }
}