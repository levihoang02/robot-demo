package com.example.robotdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robotdemo.UART.SerialState

@Composable
fun UartStatusChip(
    state: SerialState,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (state) {
        is SerialState.Idle -> "UART Idle" to Color.Gray
        is SerialState.Connecting -> "Connecting..." to Color(0xFFFFA000)
        is SerialState.Connected -> "Connected" to Color(0xFF4CAF50)
        is SerialState.Reconnecting -> "Reconnecting..." to Color(0xFFFF9800)
        is SerialState.Error -> "UART Error" to Color.Red
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
