package com.example.robotdemo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robotdemo.qna.VoiceState

@Composable
fun VoiceAssistantOverlay(
    voiceState: VoiceState,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = voiceState !is VoiceState.Idle,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // WAVE ANIMATION
                Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition(label = "wave")
                    val waveScale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "scale"
                    )
                    val waveAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.7f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )

                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF1976D2)))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer(scaleX = waveScale, scaleY = waveScale)
                            .alpha(waveAlpha)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2))
                    )
                }

                Spacer(modifier = Modifier.height(64.dp))

                Text(
                    text = when (voiceState) {
                        is VoiceState.Listening -> "Đang nghe bạn nói..."
                        is VoiceState.Processing -> "Đang suy nghĩ..."
                        is VoiceState.Speaking -> voiceState.response
                        is VoiceState.Error -> "Lỗi: ${voiceState.message}"
                        else -> ""
                    },
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 80.dp)
                )
            }

            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopStart).padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
