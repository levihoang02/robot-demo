package com.example.robotdemo.data

import android.content.Context
import com.example.robotdemo.UART.SerialState
import com.example.robotdemo.adapter.RobotController
import com.example.robotdemo.adapter.SerialRobotController
import com.example.robotdemo.domain.RobotStatus
import kotlinx.coroutines.flow.StateFlow

object RobotRepository {

    private lateinit var controllerImpl:
            RobotController

    lateinit var controller:
            RobotController
        private set

    lateinit var robotStatus:
            StateFlow<RobotStatus>
        private set

    lateinit var uartState: StateFlow<SerialState>
        private set

    fun initialize(
        context: Context
    ) {

        controllerImpl =
            SerialRobotController(
                context
            )

        controller =
            controllerImpl

        robotStatus =
            controller.robotStatus

        uartState =
            (controller as SerialRobotController)
                .uartState
    }

    fun handleIncomingStatus(
        status: RobotStatus
    ) {

        controller
            .onStatusMessageReceived(
                status
            )
    }
}