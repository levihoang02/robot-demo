package com.example.robotdemo.UART

import org.json.JSONObject

interface PacketListener {

    fun onConnected()

    fun onDisconnected()

    fun onPacketReceived(
        packet: Packet
    )

    fun onJsonReceived(
        json: JSONObject
    )

    fun onRawReceived(
        bytes: ByteArray
    )

    fun onError(
        throwable: Throwable
    )

    fun onStateChanged(
        state: SerialState
    )
}