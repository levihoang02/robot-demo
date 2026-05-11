package com.example.robotdemo.UART

object PacketSender {

    fun buildPacket(
        packet: Packet
    ): ByteArray {

        return byteArrayOf(

            '!'.code.toByte(),

            packet.header
                .toString()[0]
                .code
                .toByte(),

            packet.enumValue
                .toString()[0]
                .code
                .toByte(),

            '#'.code.toByte()
        )
    }

    fun buildPacket(
        header: Int,
        enumValue: Int
    ): ByteArray {

        return buildPacket(

            Packet(
                header,
                enumValue
            )
        )
    }
}