package com.example.robotdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.robotdemo.ui.RobotScreen
import com.example.robotdemo.ui.theme.RobotDemoTheme
import com.example.robotdemo.data.RobotRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RobotRepository.initialize(this)
        setContent {
            RobotDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RobotScreen()
                }
            }
        }
    }
}
