package com.example.test.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.domain.RobotStatus

@Composable
fun ActiveMissionScreen(
    robotStatus: RobotStatus,
    onShowDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onShowDialog() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = robotStatus.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )
                
                RobotLottieAnimation(robotStatus, modifier = Modifier.fillMaxSize(0.7f))
            }
            
            if (robotStatus == RobotStatus.BLOCKED) {
                Text(
                    text = "PATH BLOCKED",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
