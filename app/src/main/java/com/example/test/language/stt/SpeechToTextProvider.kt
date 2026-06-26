package com.example.test.language.stt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SpeechToTextProvider {

    val transcriptFlow: Flow<STTEvent>

    val stateFlow: StateFlow<STTState>

    suspend fun startListening()

    suspend fun stopListening()

    fun release()
}

sealed interface STTState {

    data object Idle : STTState

    data object Listening : STTState

    data object Processing : STTState

    data class Error(
        val code: Int,
        val message: String
    ) : STTState
}