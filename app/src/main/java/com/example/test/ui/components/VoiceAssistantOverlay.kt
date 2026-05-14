package com.example.test.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.test.qna.VoiceState

@Composable
fun VoiceAssistantOverlay(
    voiceState: VoiceState,
    isVisible: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit
) {

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                /**
                 * Wave animation only while listening
                 */
                if (voiceState is VoiceState.Listening) {

                    ListeningAnimation()
                } else {

                    IdleCircle()
                }

                Spacer(
                    modifier = Modifier.height(64.dp)
                )

                Text(
                    text = voiceText(voiceState),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                )

                /**
                 * Reply button
                 */
                if (
                    voiceState is VoiceState.WaitingForRetry
                ) {

                    Spacer(
                        modifier = Modifier.height(32.dp)
                    )

                    Button(
                        onClick = onRetry
                    ) {

                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text("Trả lời")
                    }
                }

                /**
                 * Error box
                 */
                if (voiceState is VoiceState.Error) {

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Text(
                        text = voiceState.message
                            .take(500),
                        color = Color.Red,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Button(
                        onClick = onRetry
                    ) {

                        Text("Thử lại")
                    }
                }
            }

            /**
             * Close button
             */
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
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

@Composable
private fun ListeningAnimation() {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "wave"
        )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1200,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1200,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF1976D2))
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer(
                    scaleX = waveScale,
                    scaleY = waveScale
                )
                .alpha(waveAlpha)
                .clip(CircleShape)
                .background(Color(0xFF1976D2))
        )
    }
}

@Composable
private fun IdleCircle() {

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
    )
}

private fun voiceText(
    voiceState: VoiceState
): String {

    return when (voiceState) {

        is VoiceState.Listening ->
            "Đang nghe bạn nói..."

        is VoiceState.WaitingForRetry ->
            "Đang nghe bạn nói..."

        is VoiceState.Processing ->
            "Đang suy nghĩ..."

        is VoiceState.Speaking ->
            voiceState.response

        is VoiceState.Error ->
            "Đã xảy ra lỗi!"

        else ->
            ""
    }
}