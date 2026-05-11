package com.example.robotdemo.UART

object Protocol {

    // Header

    const val HEADER_CONTROLLER = 0x01
    const val HEADER_COLLISION = 0x02
    const val HEADER_TARGET = 5

    // Controller Signal

    const val CONTROLLER_RECEIVED_GOAL = 0x01
    const val CONTROLLER_NEW_PATH = 0x02
    const val CONTROLLER_REACHED_GOAL = 0x03

    // Collision

    const val COLLISION_NORMAL = 0x01
    const val COLLISION_SLOWDOWN = 0x02
    const val COLLISION_STOP = 0x03

    // Target Point
    const val TARGET_HOME = 0
    const val TARGET_1 = 1
    const val TARGET_2 = 2
    const val TARGET_3 = 3
}