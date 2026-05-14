package com.example.test.UART.internal

import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WriteWorker(

    private val scope: CoroutineScope,

    private val mutex: Mutex,

    private val portProvider:
        () -> UsbSerialPort?,

    private val timeout: Int,

    private val onError:
        (Exception) -> Unit
) {

    private val channel =
        Channel<ByteArray>(
            Channel.UNLIMITED
        )

    fun send(
        packet: ByteArray
    ) {

        channel.trySend(packet)
    }

    fun start() {

        scope.launch {

            for (packet in channel) {

                try {

                    mutex.withLock {

                        portProvider()
                            ?.write(
                                packet,
                                timeout
                            )
                    }

                } catch (e: Exception) {

                    onError(e)
                }
            }
        }
    }
}