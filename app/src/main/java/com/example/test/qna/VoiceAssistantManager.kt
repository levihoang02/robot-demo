package com.example.test.qna

import android.util.Log
import com.example.test.qna.domain.VoiceRequest
import com.example.test.qna.llm.LLMProvider
import com.example.test.qna.stt.SpeechToTextProvider
import com.example.test.qna.stt.STTEvent
import com.example.test.qna.tts.TextToSpeechProvider
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

        stt.transcriptFlow.collectLatest { event ->

            when (event) {

                /**
                 * User speech recognized
                 */
                is STTEvent.Transcript -> {

                    if (event.text.isBlank()) {

                        _voiceState.value =
                            VoiceState.WaitingForRetry

                        return@collectLatest
                    }

                    try {

                        _voiceState.value =
                            VoiceState.Processing

                        val response =
                            llm.generateReply(
                                VoiceRequest(
                                    event.text
                                )
                            )

                        _voiceState.value =
                            VoiceState.Speaking(
                                response.text
                            )

                        tts.speak(response)

                        /**
                         * After speaking finished
                         * return to waiting state
                         */
                        _voiceState.value =
                            VoiceState.WaitingForRetry

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "LLM/TTS Error",
                            e
                        )

                        _voiceState.value =
                            VoiceState.Error(
                                e.stackTraceToString()
                            )
                    }
                }

                /**
                 * No voice detected
                 * after timeout
                 */
                STTEvent.SilenceTimeout -> {

                    _voiceState.value =
                        VoiceState.WaitingForRetry
                }

                /**
                 * STT error
                 */
                is STTEvent.Error -> {

                    Log.e(
                        TAG,
                        "STT Error: ${event.message}"
                    )

                    _voiceState.value =
                        VoiceState.Error(
                            buildString {

                                appendLine(
                                    "STT Error"
                                )

                                appendLine()

                                appendLine(
                                    "Code: ${event.code}"
                                )

                                appendLine(
                                    "Message: ${event.message}"
                                )
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

    fun closeAssistant() {

        _voiceState.value =
            VoiceState.Idle
    }
}