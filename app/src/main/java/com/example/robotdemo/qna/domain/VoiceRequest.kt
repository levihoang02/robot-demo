package com.example.robotdemo.qna.domain

data class VoiceRequest(

    val text: String,

    val timestamp: Long =
        System.currentTimeMillis(),

    val language: String = "vi"
)
