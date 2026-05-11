package com.example.robotdemo.UART

object Parser {

    fun parsePacket(
        buffer: ByteArray
    ): Packet? {

        if (buffer.size < 4) {
            return null
        }

        val start =
            buffer.indexOf(
                '!'.code.toByte()
            )

        val end =
            buffer.indexOf(
                '#'.code.toByte()
            )

        if (
            start == -1 ||
            end == -1 ||
            end <= start
        ) {
            return null
        }

        val packet =
            buffer.copyOfRange(
                start,
                end + 1
            )

        if (packet.size != 4) {
            return null
        }

        val headerChar =
            packet[1]
                .toInt()
                .toChar()

        val enumChar =
            packet[2]
                .toInt()
                .toChar()

        if (
            !headerChar.isDigit() ||
            !enumChar.isDigit()
        ) {
            return null
        }

        return Packet(

            header =
                headerChar.digitToInt(),

            enumValue =
                enumChar.digitToInt()
        )
    }
}