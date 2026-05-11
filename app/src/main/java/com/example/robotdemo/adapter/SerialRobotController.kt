package com.example.robotdemo.adapter

import android.content.Context
import com.example.robotdemo.UART.Packet
import com.example.robotdemo.UART.PacketListener
import com.example.robotdemo.UART.Protocol
import com.example.robotdemo.UART.SerialState
import com.example.robotdemo.UART.UsbSerialManager
import com.example.robotdemo.domain.NavigationPoint
import com.example.robotdemo.domain.RobotStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

import android.util.Log

class SerialRobotController(
    context: Context
) : RobotController {

    private val _uartState =
        MutableStateFlow<SerialState>(
            SerialState.Idle
        )

    val uartState: StateFlow<SerialState> =
        _uartState.asStateFlow()

    private val _robotStatus =

        MutableStateFlow(
            RobotStatus.IDLE
        )

    override val robotStatus:
            StateFlow<RobotStatus> =
        _robotStatus.asStateFlow()

    init {
        Log.d("UART_DEBUG", "SerialRobotController init")

        _uartState.value =
            SerialState.Connecting

        try {

            Log.d("UART_DEBUG", "Initializing USB")

            UsbSerialManager.initialize(context)

            Log.d("UART_DEBUG", "USB initialized")

            _uartState.value =
                SerialState.Connected

            Log.d(
                "UART_DEBUG",
                "State manually set CONNECTED"
            )

        } catch (e: Exception) {

            Log.e(
                "UART_DEBUG",
                "UART init failed",
                e
            )

            _uartState.value =
                SerialState.Error(
                    e.message ?: "UART init failed"
                )
        }

        UsbSerialManager.setListener(

            object : PacketListener {

                override fun onConnected() {

                    Log.d(
                        "UART_DEBUG",
                        "onConnected callback"
                    )

                    _uartState.value =
                        SerialState.Connected
                }

                override fun onDisconnected() {

                    Log.d(
                        "UART_DEBUG",
                        "onDisconnected callback"
                    )

                    _uartState.value =
                        SerialState.Idle
                }

                override fun onPacketReceived(
                    packet: Packet
                ) {

                    parseRobotPacket(
                        packet
                    )
                }

                override fun onJsonReceived(
                    json: JSONObject
                ) {

                    println(
                        json.toString(4)
                    )
                }

                override fun onRawReceived(
                    bytes: ByteArray
                ) {
                }

                override fun onError(
                    throwable: Throwable
                ) {

                    throwable.printStackTrace()
                }

                override fun onStateChanged(
                    state: SerialState
                ) {

                    Log.d(
                        "UART_DEBUG",
                        "onStateChanged: $state"
                    )

                    _uartState.value = state
                }
            }
        )
        Log.d(
            "UART_DEBUG",
            "Calling connect()"
        )
        UsbSerialManager.connect()
    }

    override fun sendTargetCoordinates(
        point: NavigationPoint
    ) {

        val target =
            when (point.name) {

                "HOME" ->
                    Protocol.TARGET_HOME

                "TARGET_1" ->
                    Protocol.TARGET_1

                "TARGET_2" ->
                    Protocol.TARGET_2

                "TARGET_3" ->
                    Protocol.TARGET_3

                else ->
                    Protocol.TARGET_HOME
            }

        UsbSerialManager.sendPacket(

            Protocol.HEADER_TARGET,

            target
        )

        _robotStatus.value =
            RobotStatus.MOVING
    }

    private fun parseRobotPacket(
        packet: Packet
    ) {

        when (packet.header) {

            Protocol.HEADER_CONTROLLER -> {

                when (
                    packet.enumValue
                ) {

                    Protocol.CONTROLLER_RECEIVED_GOAL -> {

                        println(
                            "Robot received goal"
                        )
                    }

                    Protocol.CONTROLLER_NEW_PATH -> {

                        _robotStatus.value =
                            RobotStatus.MOVING
                    }

                    Protocol.CONTROLLER_REACHED_GOAL -> {

                        _robotStatus.value =
                            RobotStatus.IDLE
                    }
                }
            }

            Protocol.HEADER_COLLISION -> {

                when (
                    packet.enumValue
                ) {

                    Protocol.COLLISION_NORMAL -> {

                        _robotStatus.value =
                            RobotStatus.MOVING
                    }

                    Protocol.COLLISION_SLOWDOWN -> {

                        _robotStatus.value =
                            RobotStatus.AVOIDING
                    }

                    Protocol.COLLISION_STOP -> {

                        _robotStatus.value =
                            RobotStatus.BLOCKED
                    }
                }
            }
        }
    }

    override fun onStatusMessageReceived(
        status: RobotStatus
    ) {

        _robotStatus.value = status
    }


    override fun stop() {

        UsbSerialManager.disconnect()

        _robotStatus.value =
            RobotStatus.IDLE
    }
}