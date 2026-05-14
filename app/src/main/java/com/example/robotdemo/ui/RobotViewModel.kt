package com.example.robotdemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.robotdemo.data.RobotRepository
import com.example.robotdemo.domain.NavigationPoint
import com.example.robotdemo.domain.RobotStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.example.robotdemo.UART.Protocol
import com.example.robotdemo.UART.SerialState

class RobotViewModel : ViewModel() {
    // UI now observes the persistent Global State
    val robotStatus: StateFlow<RobotStatus> = RobotRepository.robotStatus

    val uartState: StateFlow<SerialState> = RobotRepository.uartState

    private val _navigationPoints = MutableStateFlow(
        listOf(
            NavigationPoint("1", "A", Protocol.TARGET_1),
            NavigationPoint("2", "B", Protocol.TARGET_2),
            NavigationPoint("3", "C", Protocol.TARGET_3),
            NavigationPoint("4", "D", Protocol.TARGET_4),
            NavigationPoint("5", "E", Protocol.TARGET_5),
        )
    )
    val navigationPoints: StateFlow<List<NavigationPoint>> = _navigationPoints.asStateFlow()

    fun onPointSelected(point: NavigationPoint) {
        RobotRepository.controller.sendTargetCoordinates(point)
    }

    fun stopRobot() {
        RobotRepository.controller.stop()
    }
}
