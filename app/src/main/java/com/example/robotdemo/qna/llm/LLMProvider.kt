package com.example.robotdemo.qna.llm

import com.example.robotdemo.qna.domain.VoiceRequest
import com.example.robotdemo.qna.domain.VoiceResponse

interface LLMProvider {

    suspend fun generateReply(
        request: VoiceRequest
    ): VoiceResponse
}