package com.example.test.qna

import android.util.Log
import com.example.test.qna.domain.VoiceRequest
import com.example.test.qna.llm.LLMProvider
import com.example.test.qna.stt.SpeechToTextProvider
import com.example.test.qna.stt.STTEvent
import com.example.test.qna.tts.TextToSpeechProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceAssistantManager(
    private val stt: SpeechToTextProvider,
    private val llm: LLMProvider,
    private val tts: TextToSpeechProvider
) {

    companion object {
        private const val TAG = "AndroidSTT"
    }

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState = _voiceState.asStateFlow()

    /**
     * The current job handling an STT event (LLM + TTS).
     * We use this to manually manage cancellations when a new transcript arrives.
     */
    private var currentProcessingJob: Job? = null

    suspend fun start() = coroutineScope {
        stt.transcriptFlow.collect { event ->
            Log.d(TAG, "New STT Event: $event")
            
            when (event) {
                is STTEvent.Transcript -> {
                    // New speech detected! Cancel any previous thinking/speaking.
                    currentProcessingJob?.cancel()
                    
                    currentProcessingJob = launch {
                        handleTranscript(event.text)
                    }
                }
                
                STTEvent.SilenceTimeout -> {
                    // Silence timeout should only trigger a retry if we are actually listening.
                    // It should NOT cancel a current LLM/TTS process.
                    if (_voiceState.value == VoiceState.Listening) {
                        _voiceState.value = VoiceState.WaitingForRetry
                    }
                }
                
                is STTEvent.Error -> {
                    // Only show errors if we aren't currently doing something more important,
                    // or if the error is critical enough to stop everything.
                    if (_voiceState.value == VoiceState.Listening) {
                        Log.e(TAG, "STT Error while listening: ${event.message}")
                        _voiceState.value = VoiceState.Error(
                            "STT Error: ${event.message} (Code: ${event.code})"
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleTranscript(text: String) {
        if (text.isBlank()) {
            _voiceState.value = VoiceState.WaitingForRetry
            return
        }

        try {
            _voiceState.value = VoiceState.Processing
            
            val response = llm.generateReply(VoiceRequest(text))
            
            // Check if we were cancelled while waiting for LLM
            _voiceState.value = VoiceState.Speaking(response.text)
            
            tts.speak(response)
            
            // If we finished speaking normally, wait for the next command
            _voiceState.value = VoiceState.WaitingForRetry

        } catch (e: Exception) {
            // Exceptions from LLM or TTS
            if (_voiceState.value != VoiceState.Idle) {
                Log.e(TAG, "Processing Error", e)
                _voiceState.value = VoiceState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun startListening() {
        // Reset state and stop any current speaking if user manually triggers mic
        currentProcessingJob?.cancel()
        tts.stop()
        
        _voiceState.value = VoiceState.Listening
        stt.startListening()
    }

    suspend fun stopListening() {
        stt.stopListening()
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.WaitingForRetry
        }
    }

    suspend fun closeAssistant() {
        currentProcessingJob?.cancel()
        stt.stopListening()
        tts.stop()
        _voiceState.value = VoiceState.Idle
    }
}
