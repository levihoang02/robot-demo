package com.example.robotdemo.qna

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class Speaking(val response: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}
