package com.example.test.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.*
import com.example.test.domain.RobotStatus

@Composable
fun RobotLottieAnimation(status: RobotStatus, modifier: Modifier = Modifier.size(350.dp)) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.example.test.R.raw.robot_cat))
    val isPreview = LocalInspectionMode.current
    
    // Dynamic emotion based on status
    val colorFilter = when (status) {
        RobotStatus.BLOCKED -> Color.Red // Hint of red for anger/blocked
        else -> Color.White
    }

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(colorFilter.toArgb()),
            keyPath = arrayOf("**")
        )
    )

    val speed = when (status) {
        RobotStatus.MOVING -> 1.5f
        RobotStatus.BLOCKED -> 0.4f // Slower, frustrated movement
        else -> 1.0f
    }

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = speed
    )

    LottieAnimation(
        composition = composition,
        progress = { if (isPreview) 0.5f else progress },
        modifier = modifier,
        dynamicProperties = dynamicProperties
    )
}
