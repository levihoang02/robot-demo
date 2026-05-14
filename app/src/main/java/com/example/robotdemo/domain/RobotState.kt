package com.example.robotdemo.domain

enum class RobotStatus {
    IDLE,
    MOVING,
    AVOIDING,
    BLOCKED,
    SLEEPING,
    GOAL_REACHED,
}

data class RobotState(
    val status: RobotStatus = RobotStatus.MOVING,
    val currentPosition: String? = null,
    val targetPosition: String? = null
)
