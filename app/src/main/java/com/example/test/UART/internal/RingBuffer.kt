package com.example.test.UART.internal

class RingBuffer(

    private val size: Int
) {

    private val buffer =
        ByteArray(size)

    private var writeIndex = 0

    fun write(data: ByteArray) {

        for (byte in data) {

            buffer[
                writeIndex % size
            ] = byte

            writeIndex++
        }
    }

    fun snapshot(): ByteArray {

        return buffer.copyOf()
    }
}