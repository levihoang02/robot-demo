package com.example.robotdemo.UART

object PacketReceiver {

    fun parse(
        bytes: ByteArray
    ): Packet? {

        return Parser.parsePacket(
            bytes
        )
    }

    fun toJson(
        packet: Packet
    ) = JsonMapper.packetToJson(
        packet
    )
}