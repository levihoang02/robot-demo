package com.example.test.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.UART.SerialState

@Composable
fun UartStatusChip(
    state: SerialState,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (state) {
        is SerialState.Idle -> "UART IDLE" to Color.Gray
        is SerialState.Connecting -> "CONNECTING" to Color(0xFFFFA000)
        is SerialState.Connected -> "CONNECTED" to Color(0xFF4CAF50)
        is SerialState.Reconnecting -> "RECONNECTING" to Color(0xFFFF9800)
        is SerialState.Error -> "UART ERROR" to Color.Red
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Status Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Small Status Text
        Text(
            text = text,
            color = Color.Black.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}