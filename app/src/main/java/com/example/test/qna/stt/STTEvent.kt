package com.example.test.qna.stt

sealed interface STTEvent {

    data class Transcript(
        val text: String
    ) : STTEvent

    data object SilenceTimeout : STTEvent

    data class Error(
        val code: Int,
        val message: String
    ) : STTEvent
}