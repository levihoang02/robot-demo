package com.example.test.UART

import org.json.JSONObject

object JsonMapper {

    fun packetToJson(
        packet: Packet
    ): JSONObject {

        val type: String

        val status: String

        when (packet.header) {

            Protocol.HEADER_CONTROLLER -> {

                type =
                    "controller_signal"

                status =
                    when (
                        packet.enumValue
                    ) {

                        Protocol.CONTROLLER_RECEIVED_GOAL ->
                            "received_goal"

                        Protocol.CONTROLLER_NEW_PATH ->
                            "passing_new_path"

                        Protocol.CONTROLLER_REACHED_GOAL ->
                            "reached_goal"

                        else ->
                            "unknown"
                    }
            }

            Protocol.HEADER_COLLISION -> {

                type = "collision"

                status =
                    when (
                        packet.enumValue
                    ) {

                        Protocol.COLLISION_NORMAL ->
                            "normal_velocity"

                        Protocol.COLLISION_SLOWDOWN ->
                            "slowdown"

                        Protocol.COLLISION_STOP ->
                            "stop"

                        else ->
                            "unknown"
                    }
            }

            else -> {

                type = "unknown"

                status = "unknown"
            }
        }

        return JSONObject().apply {

            put("type", type)

            put("status", status)
        }
    }
}