package com.example.robotdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.robotdemo.UART.Protocol
import com.example.robotdemo.UART.SerialState
import com.example.robotdemo.adapter.VoiceNotifier
import com.example.robotdemo.domain.NavigationPoint
import com.example.robotdemo.domain.RobotStatus
import com.example.robotdemo.qna.QnARepository
import com.example.robotdemo.qna.VoiceState
import com.example.robotdemo.ui.components.*
import com.example.robotdemo.ui.theme.RobotDemoTheme
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotScreen(viewModel: RobotViewModel = viewModel()) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val voiceNotifier = remember(context) { 
        if (isPreview) null else VoiceNotifier(context) 
    }
    val robotStatus by viewModel.robotStatus.collectAsState()
    val navPoints by viewModel.navigationPoints.collectAsState()
    val uartState by viewModel.uartState.collectAsState()
    val voiceState by QnARepository.voiceState.collectAsState()
    
    LaunchedEffect(robotStatus) {
        voiceNotifier?.notifyStatus(robotStatus)
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceNotifier?.shutdown()
        }
    }

    RobotScreenContent(
        robotStatus = robotStatus,
        uartState = uartState,
        voiceState = voiceState,
        navPoints = navPoints,
        onPointSelected = { viewModel.onPointSelected(it) },
        onStopRobot = { viewModel.stopRobot() },
        onMicClicked = {
            if (voiceState is VoiceState.Listening) {
                QnARepository.stopListening()
            } else {
                QnARepository.startListening()
            }
        },
        onCloseAssistant = { QnARepository.closeAssistant() }
    )
}

@Composable
fun RobotScreenContent(
    robotStatus: RobotStatus,
    uartState: SerialState,
    voiceState: VoiceState,
    navPoints: List<NavigationPoint>,
    onPointSelected: (NavigationPoint) -> Unit,
    onStopRobot: () -> Unit,
    onMicClicked: () -> Unit,
    onCloseAssistant: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedPoints by remember { mutableStateOf(listOf<NavigationPoint>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (robotStatus == RobotStatus.IDLE) Color(0xFFF8F9FA) else Color.Black)
    ) {
        if (robotStatus == RobotStatus.IDLE) {
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT: SELECTION STACK
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(32.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(32.dp))
                        .padding(24.dp)
                ) {
                    Text("SELECTION STACK", style = MaterialTheme.typography.labelLarge, color = Color(0xFF74777F))
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedPoints.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No points selected", color = Color.LightGray)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                selectedPoints.forEachIndexed { index, point ->
                                    Surface(
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = point.name, color = Color(0xFF1976D2), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { selectedPoints = selectedPoints - point }) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            onPointSelected(NavigationPoint("home", "Home", Protocol.TARGET_HOME))
                            selectedPoints = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238), contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
                            Text("RETURN HOME", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // RIGHT: POINTS TABLE AND START
                Column(
                    modifier = Modifier.weight(1.2f).fillMaxHeight().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DESTINATIONS", style = MaterialTheme.typography.labelLarge, color = Color(0xFF74777F))
                        
                        FilledTonalIconButton(
                            onClick = onMicClicked,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (voiceState is VoiceState.Listening) Color.Red else Color(0xFFE3F2FD),
                                contentColor = if (voiceState is VoiceState.Listening) Color.White else Color(0xFF1976D2)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(navPoints) { point ->
                            CompactDestinationCard(point.name) { 
                                if (!selectedPoints.contains(point)) selectedPoints = selectedPoints + point 
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            if (selectedPoints.isNotEmpty()) {
                                onPointSelected(selectedPoints.first())
                                selectedPoints = emptyList() 
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        enabled = selectedPoints.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White, disabledContainerColor = Color(0xFFE0E0E0), disabledContentColor = Color.White),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("START MISSION", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Full Screen Lottie for Active States
            Box(modifier = Modifier.fillMaxSize().clickable { showConfirmDialog = true }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = robotStatus.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp, textAlign = TextAlign.Center)
                        RobotLottieAnimation(robotStatus, modifier = Modifier.fillMaxSize(0.7f))
                    }
                    if (robotStatus == RobotStatus.BLOCKED) {
                        Text(text = "PATH BLOCKED", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }

        UartStatusChip(state = uartState, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))

        VoiceAssistantOverlay(voiceState = voiceState, onClose = onCloseAssistant)

        if (showConfirmDialog) {
            ElegantGlassDialog(onDismiss = { showConfirmDialog = false }, onConfirm = { onStopRobot(); showConfirmDialog = false })
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800, name = "Modern Idle")
@Composable
fun PreviewModernIdle() {
    RobotDemoTheme {
        RobotScreenContent(
            robotStatus = RobotStatus.IDLE,
            uartState = SerialState.Idle,
            voiceState = VoiceState.Idle,
            navPoints = listOf(
                NavigationPoint("1", "A", Protocol.TARGET_1), NavigationPoint("2", "B", Protocol.TARGET_2),
                NavigationPoint("3", "C", Protocol.TARGET_3)
            ),
            onPointSelected = {},
            onStopRobot = {},
            onMicClicked = {},
            onCloseAssistant = {}
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800, name = "QnA Speaking")
@Composable
fun PreviewQnASpeaking() {
    RobotDemoTheme {
        RobotScreenContent(
            robotStatus = RobotStatus.IDLE,
            uartState = SerialState.Connected,
            voiceState = VoiceState.Speaking("Tôi là Robot Demo, tôi có thể giúp gì cho bạn?"),
            navPoints = emptyList(),
            onPointSelected = {},
            onStopRobot = {},
            onMicClicked = {},
            onCloseAssistant = {}
        )
    }
}
