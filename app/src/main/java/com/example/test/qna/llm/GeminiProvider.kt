package com.example.test.qna.llm

import android.util.Log
import com.example.test.qna.domain.VoiceRequest
import com.example.test.qna.domain.VoiceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GeminiProvider(
    private val apiKey: String
) : LLMProvider {

    companion object {
        private const val TAG = "AndroidSTT"
    }

    private val client = OkHttpClient()

    override suspend fun generateReply(
        request: VoiceRequest
    ): VoiceResponse = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()

        Log.d(TAG, "▶️ Request text: ${request.text}")

        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {

            put("system_instruction", JSONObject().apply {
                put("parts", org.json.JSONArray().put(
                    JSONObject().put(
                        "text",
                        "Bạn là trợ lý giọng nói tiếng Việt. Trả lời tối đa 1-2 câu, ngắn gọn."
                    )
                ))
            })

            put("contents", org.json.JSONArray().put(
                JSONObject().apply {
                    put("parts", org.json.JSONArray().put(
                        JSONObject().put("text", request.text)
                    ))
                }
            ))
        }
        Log.d(TAG, jsonBody.toString(2))
        val bodyString = jsonBody.toString()

        Log.d(TAG, "📤 Request JSON: $bodyString")

        val body = bodyString
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return@withContext try {

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            val endTime = System.currentTimeMillis()

            Log.d(TAG, "⏱ Latency: ${endTime - startTime}ms")
            Log.d(TAG, "📥 HTTP Code: ${response.code}")
            Log.d(TAG, "📥 Raw Response: $responseBody")

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "❌ Request failed")
                return@withContext VoiceResponse(
                    text = "Xin lỗi, tôi không thể xử lý yêu cầu lúc này."
                )
            }

            val json = JSONObject(responseBody)

            val resultText =
                json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

            Log.d(TAG, "✅ Parsed result: $resultText")

            VoiceResponse(text = resultText)

        } catch (e: Exception) {

            Log.e(TAG, "💥 Exception: ${e.message}", e)

            VoiceResponse(
                text = "Có lỗi xảy ra khi gọi AI."
            )
        }
    }
}