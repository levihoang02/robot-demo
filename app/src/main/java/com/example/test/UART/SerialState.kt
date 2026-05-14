package com.example.test.UART

sealed class SerialState {

    object Idle : SerialState()

    object Connecting : SerialState()

    object Connected : SerialState()

    object Reconnecting : SerialState()

    data class Error(
        val message: String
    ) : SerialState()
}