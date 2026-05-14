package com.example.test.UART

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.example.test.UART.internal.ReadWorker
import com.example.test.UART.internal.WriteWorker


import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

object UsbSerialManager {

    private const val ACTION_USB_PERMISSION =
        "com.example.test.USB_PERMISSION"

    private lateinit var appContext: Context

    private var config =
        UsbSerialConfig()

    private var listener:
            PacketListener? = null

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )

    private val mutex =
        Mutex()

    @Volatile
    private var serialPort:
            UsbSerialPort? = null

    private val isReceiverRegistered =
        AtomicBoolean(false)

    private val isReconnecting =
        AtomicBoolean(false)

    private val isInitialized =
        AtomicBoolean(false)

    private val isReadWorkerStarted =
        AtomicBoolean(false)

    val stateFlow =
        MutableStateFlow<SerialState>(
            SerialState.Idle
        )

    private lateinit var parser:
            PacketParser

    private lateinit var writeWorker:
            WriteWorker

    private lateinit var readWorker:
            ReadWorker

    private lateinit var retryPolicy:
            RetryPolicy

    fun initialize(
        context: Context,
        config: UsbSerialConfig =
            UsbSerialConfig()
    ) {

        if (isInitialized.get()) {
            return
        }

        appContext =
            context.applicationContext

        this.config = config

        retryPolicy =
            RetryPolicy(
                config.maxRetryCount,
                config.reconnectDelayMs
            )

        parser =
            PacketParser { raw ->

                listener?.onRawReceived(
                    raw
                )

                val packet =
                    Parser.parsePacket(raw)

                if (packet != null) {

                    listener?.onPacketReceived(
                        packet
                    )

                    listener?.onJsonReceived(
                        JsonMapper.packetToJson(
                            packet
                        )
                    )
                }
            }

        writeWorker =
            WriteWorker(
                scope,
                mutex,
                { serialPort },
                config.writeTimeout
            ) {

                reconnect(it)
            }

        readWorker =
            ReadWorker(
                scope,
                mutex,
                { serialPort },
                config.readTimeout,
                {
                    parser.append(it)
                }
            ) {

                reconnect(it)
            }

        writeWorker.start()

        registerReceiver()

        isInitialized.set(true)
    }

    fun setListener(
        listener: PacketListener
    ) {

        this.listener = listener
    }

    fun connect() {

        if (!isInitialized.get()) {

            updateState(
                SerialState.Error(
                    "UsbSerialManager not initialized"
                )
            )

            return
        }

        scope.launch {

            try {

                updateState(
                    SerialState.Connecting
                )

                val manager =
                    appContext.getSystemService(
                        Context.USB_SERVICE
                    ) as UsbManager

                val drivers =
                    UsbSerialProber
                        .getDefaultProber()
                        .findAllDrivers(manager)

                if (drivers.isEmpty()) {

                    updateState(
                        SerialState.Error(
                            "No USB Device"
                        )
                    )

                    return@launch
                }

                val driver =
                    drivers.first()

                val permissionIntent =
                    PendingIntent.getBroadcast(
                        appContext,
                        0,
                        Intent(
                            ACTION_USB_PERMISSION
                        ).setPackage(
                            appContext.packageName
                        ),
                        PendingIntent.FLAG_MUTABLE
                    )

                manager.requestPermission(
                    driver.device,
                    permissionIntent
                )

            } catch (e: Exception) {

                reconnect(e)
            }
        }
    }

    private fun registerReceiver() {

        if (
            isReceiverRegistered.get()
        ) {
            return
        }

        ContextCompat.registerReceiver(
            appContext,
            usbReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        isReceiverRegistered.set(true)
    }

    private val usbReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action !=
                    ACTION_USB_PERMISSION
                ) {
                    return
                }

                val granted =
                    intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED,
                        false
                    )

                if (!granted) {

                    updateState(
                        SerialState.Error(
                            "USB Permission Denied"
                        )
                    )

                    return
                }

                val device =
                    IntentCompat.getParcelableExtra(
                        intent,
                        UsbManager.EXTRA_DEVICE,
                        UsbDevice::class.java
                    ) ?: return

                scope.launch {

                    openDevice(device)
                }
            }
        }

    private suspend fun openDevice(
        device: UsbDevice
    ) {

        try {

            val manager =
                appContext.getSystemService(
                    Context.USB_SERVICE
                ) as UsbManager

            val driver =
                UsbSerialProber
                    .getDefaultProber()
                    .probeDevice(device)
                    ?: throw Exception(
                        "No Driver"
                    )

            val connection =
                manager.openDevice(device)
                    ?: throw Exception(
                        "Open Failed"
                    )

            val port =
                driver.ports.first()

            mutex.withLock {

                serialPort?.close()

                serialPort = port

                port.open(connection)

                port.setParameters(
                    config.baudRate,
                    config.dataBits,
                    config.stopBits,
                    config.parity
                )
            }

            if (
                !isReadWorkerStarted.get()
            ) {

                readWorker.start()

                isReadWorkerStarted.set(true)
            }

            updateState(
                SerialState.Connected
            )

            listener?.onConnected()

            isReconnecting.set(false)

        } catch (e: Exception) {

            reconnect(e)
        }
    }

    fun send(
        packet: ByteArray
    ) {

        if (serialPort == null) {

            listener?.onError(
                Exception(
                    "Serial port not connected"
                )
            )

            return
        }

        writeWorker.send(packet)
    }

    fun sendPacket(
        header: Int,
        enumValue: Int
    ) {

        val bytes =
            PacketSender.buildPacket(
                header,
                enumValue
            )

        send(bytes)
    }

    fun disconnect() {

        scope.launch {

            mutex.withLock {

                try {

                    serialPort?.close()

                } catch (_: Exception) {
                }

                serialPort = null
            }

            updateState(
                SerialState.Idle
            )

            listener?.onDisconnected()
        }
    }

    fun release() {

        disconnect()

        scope.coroutineContext
            .cancelChildren()

        try {

            if (
                isReceiverRegistered.get()
            ) {

                appContext.unregisterReceiver(
                    usbReceiver
                )

                isReceiverRegistered.set(false)
            }

        } catch (_: Exception) {
        }

        isInitialized.set(false)

        isReadWorkerStarted.set(false)
    }

    private fun reconnect(
        exception: Exception
    ) {

        if (
            isReconnecting.get()
        ) {
            return
        }

        isReconnecting.set(true)

        listener?.onError(exception)

        updateState(
            SerialState.Reconnecting
        )

        scope.launch {

            disconnect()

            retryPolicy.retry {

                connect()
            }

            isReconnecting.set(false)
        }
    }

    private fun updateState(
        state: SerialState
    ) {

        stateFlow.value = state

        listener?.onStateChanged(state)
    }
}