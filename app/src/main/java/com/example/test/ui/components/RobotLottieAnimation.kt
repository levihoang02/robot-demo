package com.example.test.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun RobotLottieAnimation(
    status: RobotStatus,
    modifier: Modifier = Modifier.size(350.dp)
) {
    val rawRes = remember(status) {
        when (status) {
            RobotStatus.IDLE -> com.example.test.R.raw.robot_cat
            RobotStatus.MOVING -> com.example.test.R.raw.robot_cat
            RobotStatus.AVOIDING -> com.example.test.R.raw.robot_cat
            RobotStatus.BLOCKED -> com.example.test.R.raw.robot_cat
            RobotStatus.GOAL_REACHED -> com.example.test.R.raw.robot_cat
            else -> com.example.test.R.raw.robot_cat
        }
    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(rawRes)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = true,
        iterations = LottieConstants.IterateForever,
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier

        )
    }
}
