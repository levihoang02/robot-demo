package com.example.robotdemo.qna.tts

import com.example.robotdemo.qna.domain.VoiceResponse

interface TextToSpeechProvider {

    suspend fun speak(
        response: VoiceResponse
    )

    fun stop()
}