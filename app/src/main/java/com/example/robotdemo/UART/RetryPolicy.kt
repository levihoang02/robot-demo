package com.example.robotdemo.UART

import kotlinx.coroutines.delay

class RetryPolicy(

    private val maxRetry: Int,

    private val delayMs: Long
) {

    suspend fun retry(
        block: suspend () -> Unit
    ) {

        repeat(maxRetry) { attempt ->

            try {

                block()

                return

            } catch (e: Exception) {

                delay(
                    delayMs *
                            (attempt + 1)
                )
            }
        }
    }
}