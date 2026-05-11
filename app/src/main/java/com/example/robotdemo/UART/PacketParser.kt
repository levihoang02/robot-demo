package com.example.robotdemo.UART

class PacketParser(

    private val callback:
        (ByteArray) -> Unit
) {

    private val buffer =
        mutableListOf<Byte>()

    fun append(bytes: ByteArray) {

        synchronized(buffer) {

            buffer.addAll(bytes.toList())

            parse()
        }
    }

    private fun parse() {

        while (true) {

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
                return
            }

            val packet =
                buffer.subList(
                    start,
                    end + 1
                ).toByteArray()

            callback(packet)

            repeat(end + 1) {
                buffer.removeAt(0)
            }
        }
    }
}