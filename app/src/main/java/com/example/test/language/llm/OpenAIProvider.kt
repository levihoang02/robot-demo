package com.example.test.language.llm

import com.example.test.qna.domain.VoiceRequest
import com.example.test.qna.domain.VoiceResponse

class OpenAIProvider : LLMProvider {

    override suspend fun generateReply(
        request: VoiceRequest
    ): VoiceResponse {

        val input = request.text.lowercase()
        val result = when {
            input.contains("xin chào") || input.contains("hello") -> 
                "Xin chào! Tôi là trợ lý thư viện của bạn. Tôi có thể giúp gì cho bạn?"
            input.contains("tên") -> 
                "Tôi là Demo, được thiết kế để hỗ trợ bạn."
            input.contains("khỏe không") -> 
                "Tôi cảm thấy rất tuyệt vời! Sẵn sàng phục vụ bạn."
            input.contains("di chuyển") || input.contains("đi đến") -> 
                "Vui lòng trên màn hình để tôi bắt đầu."
            else -> "Cảm ơn bạn đã nói: ${request.text}. Tôi đang học thêm để có thể trả lời tốt hơn."
        }

        return VoiceResponse(
            text = result
        )
    }
}