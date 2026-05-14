package com.example.test.qna

sealed interface VoiceState {

    data object Idle : VoiceState

    data object Listening : VoiceState

    data object WaitingForRetry : VoiceState

    data object Processing : VoiceState

    data class Speaking(
        val response: String
    ) : VoiceState

    data class Error(
        val message: String
    ) : VoiceState
}
