package com.example.robotdemo.qna.llm

import com.example.robotdemo.qna.domain.VoiceRequest
import com.example.robotdemo.qna.domain.VoiceResponse

class OpenAIProvider : LLMProvider {

    override suspend fun generateReply(
        request: VoiceRequest
    ): VoiceResponse {

        val input = request.text.lowercase()
        val result = when {
            input.contains("xin chào") || input.contains("hello") -> 
                "Xin chào! Tôi là robot trợ lý của bạn. Tôi có thể giúp gì cho bạn?"
            input.contains("tên") -> 
                "Tôi là Robot Demo, được thiết kế để hỗ trợ bạn."
            input.contains("khỏe không") -> 
                "Tôi cảm thấy rất tuyệt vời! Sẵn sàng phục vụ bạn."
            input.contains("di chuyển") || input.contains("đi đến") -> 
                "Vui lòng chọn điểm đích trên màn hình để tôi bắt đầu di chuyển."
            else -> "Cảm ơn bạn đã nói: ${request.text}. Tôi đang học thêm để có thể trả lời tốt hơn."
        }

        return VoiceResponse(
            text = result
        )
    }
}