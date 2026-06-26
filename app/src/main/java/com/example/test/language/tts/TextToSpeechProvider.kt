package com.example.test.language.tts

import com.example.test.qna.domain.VoiceResponse

interface TextToSpeechProvider {

    suspend fun speak(
        response: VoiceResponse
    )

    fun stop()

    fun release()
}