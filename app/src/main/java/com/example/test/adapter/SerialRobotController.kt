package com.example.test.adapter

import android.content.Context
import android.util.Log
import com.example.test.UART.Packet
import com.example.test.UART.PacketListener
import com.example.test.UART.Protocol
import com.example.test.UART.SerialState
import com.example.test.UART.UsbSerialManager
import com.example.test.domain.NavigationPoint
import com.example.test.domain.RobotStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SerialRobotController(
    context: Context
) : RobotController {

    companion object {
        private const val TAG = "UART_DEBUG"
        private const val GOAL_REACHED_DELAY_MS = 2000L
    }

    // =========================
    // Coroutine
    // =========================

    private val controllerScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    private var goalReachedJob: Job? = null

    // =========================
    // State
    // =========================

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

    // Ignore packets during cooldown
    private var ignoreUntil = 0L

    // =========================
    // Init
    // =========================

    init {

        initUsb(context)

        setupListener()

        Log.d(TAG, "Calling connect()")

        UsbSerialManager.connect()
    }

    // =========================
    // Public API
    // =========================

    override fun sendTargetCoordinates(
        point: NavigationPoint
    ) {

        Log.d(
            TAG,
            "Sending target: ${point.value}"
        )

        UsbSerialManager.sendPacket(
            Protocol.HEADER_TARGET,
            point.value
        )

        updateRobotStatus(
            RobotStatus.MOVING
        )
    }

    override fun onStatusMessageReceived(
        status: RobotStatus
    ) {

        updateRobotStatus(status)
    }

    override fun stop() {

        Log.d(TAG, "Stopping controller")

        goalReachedJob?.cancel()

        controllerScope.cancel()

        UsbSerialManager.disconnect()

        updateRobotStatus(
            RobotStatus.IDLE
        )
    }

    // =========================
    // USB Setup
    // =========================

    private fun initUsb(
        context: Context
    ) {

        Log.d(TAG, "SerialRobotController init")

        updateUartState(
            SerialState.Connecting
        )

        try {

            Log.d(TAG, "Initializing USB")

            UsbSerialManager.initialize(
                context
            )

            Log.d(TAG, "USB initialized")

            updateUartState(
                SerialState.Connected
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "UART init failed",
                e
            )

            updateUartState(
                SerialState.Error(
                    e.message
                        ?: "UART init failed"
                )
            )
        }
    }

    private fun setupListener() {

        UsbSerialManager.setListener(

            object : PacketListener {

                override fun onConnected() {

                    Log.d(
                        TAG,
                        "onConnected callback"
                    )

                    updateUartState(
                        SerialState.Connected
                    )
                }

                override fun onDisconnected() {

                    Log.d(
                        TAG,
                        "onDisconnected callback"
                    )

                    updateUartState(
                        SerialState.Idle
                    )
                }

                override fun onPacketReceived(
                    packet: Packet
                ) {

                    parseRobotPacket(packet)
                }

                override fun onJsonReceived(
                    json: JSONObject
                ) {

                    Log.d(
                        TAG,
                        json.toString(4)
                    )
                }

                override fun onRawReceived(
                    bytes: ByteArray
                ) {
                    // Optional raw debug
                }

                override fun onError(
                    throwable: Throwable
                ) {

                    Log.e(
                        TAG,
                        "UART Error",
                        throwable
                    )
                }

                override fun onStateChanged(
                    state: SerialState
                ) {

                    Log.d(
                        TAG,
                        "onStateChanged: $state"
                    )

                    updateUartState(state)
                }
            }
        )
    }

    // =========================
    // Packet Parser
    // =========================

    private fun parseRobotPacket(
        packet: Packet
    ) {

        if (shouldIgnorePacket()) {

            Log.d(
                TAG,
                "Packet ignored during cooldown"
            )

            return
        }

        when (packet.header) {

            Protocol.HEADER_CONTROLLER -> {

                handleControllerPacket(
                    packet.enumValue
                )
            }

            Protocol.HEADER_COLLISION -> {

                handleCollisionPacket(
                    packet.enumValue
                )
            }
        }
    }

    private fun handleControllerPacket(
        enumValue: Int
    ) {

        when (enumValue) {

            Protocol.CONTROLLER_RECEIVED_GOAL -> {

                Log.d(
                    TAG,
                    "Goal received"
                )
            }

            Protocol.CONTROLLER_NEW_PATH -> {

                updateRobotStatus(
                    RobotStatus.MOVING
                )
            }

            Protocol.CONTROLLER_REACHED_GOAL -> {

                handleGoalReached()
            }
        }
    }

    private fun handleCollisionPacket(
        enumValue: Int
    ) {

        when (enumValue) {

            Protocol.COLLISION_NORMAL -> {

                updateRobotStatus(
                    RobotStatus.MOVING
                )
            }

            Protocol.COLLISION_SLOWDOWN -> {

                updateRobotStatus(
                    RobotStatus.AVOIDING
                )
            }

            Protocol.COLLISION_STOP -> {

                updateRobotStatus(
                    RobotStatus.BLOCKED
                )
            }
        }
    }

    // =========================
    // Goal Logic
    // =========================

    private fun handleGoalReached() {

        Log.d(TAG, "Goal reached")

        ignoreUntil =
            System.currentTimeMillis() +
                    GOAL_REACHED_DELAY_MS

        goalReachedJob?.cancel()

        goalReachedJob =
            controllerScope.launch {

                updateRobotStatus(
                    RobotStatus.GOAL_REACHED
                )

                delay(
                    GOAL_REACHED_DELAY_MS
                )

                updateRobotStatus(
                    RobotStatus.IDLE
                )
            }
    }

    private fun shouldIgnorePacket():
            Boolean {

        return System.currentTimeMillis() <
                ignoreUntil
    }

    // =========================
    // Helpers
    // =========================

    private fun updateRobotStatus(
        status: RobotStatus
    ) {

        Log.d(
            TAG,
            "RobotStatus -> $status"
        )

        _robotStatus.value = status
    }

    private fun updateUartState(
        state: SerialState
    ) {

        Log.d(
            TAG,
            "UART State -> $state"
        )

        _uartState.value = state
    }
}