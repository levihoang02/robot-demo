package com.example.robotdemo.qna.stt

import kotlinx.coroutines.flow.Flow

interface SpeechToTextProvider {

    suspend fun startListening()

    suspend fun stopListening()

    val transcriptFlow: Flow<String>
}