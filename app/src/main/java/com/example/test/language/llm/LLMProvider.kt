package com.example.test.language.llm

import com.example.test.qna.domain.VoiceRequest
import com.example.test.qna.domain.VoiceResponse

interface LLMProvider {

    suspend fun generateReply(
        request: VoiceRequest
    ): VoiceResponse
}