package com.example.robotdemo.UART.internal

import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReadWorker(

    private val scope: CoroutineScope,

    private val mutex: Mutex,

    private val portProvider:
        () -> UsbSerialPort?,

    private val timeout: Int,

    private val onReceive:
        (ByteArray) -> Unit,

    private val onError:
        (Exception) -> Unit
) {

    fun start() {

        scope.launch {

            val buffer =
                ByteArray(1024)

            while (isActive) {

                try {

                    val len =
                        mutex.withLock {

                            portProvider()
                                ?.read(
                                    buffer,
                                    timeout
                                ) ?: 0
                        }

                    if (len > 0) {

                        onReceive(
                            buffer.copyOf(len)
                        )
                    }

                } catch (e: Exception) {

                    onError(e)
                }
            }
        }
    }
}