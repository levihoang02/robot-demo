package com.example.robotdemo.adapter

import com.example.robotdemo.domain.NavigationPoint
import com.example.robotdemo.domain.RobotStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for communicating with the robot control layer.
 * This can be implemented by a concrete adapter (e.g., Serial, Socket, ROS)
 */
interface RobotController {
    val robotStatus: StateFlow<RobotStatus>
    
    // Method to send coordinates to the control board
    fun sendTargetCoordinates(point: NavigationPoint)
    
    // Callback or method to update status from control board
    fun onStatusMessageReceived(status: RobotStatus)
    
    fun stop()
}
