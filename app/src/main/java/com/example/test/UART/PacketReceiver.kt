package com.example.test.UART

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