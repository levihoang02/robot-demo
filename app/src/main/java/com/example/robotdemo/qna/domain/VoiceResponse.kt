package com.example.robotdemo.qna.domain

data class VoiceResponse(

    val text: String,

    val shouldSpeak: Boolean = true,

    val emotion: VoiceEmotion =
        VoiceEmotion.NORMAL
)
