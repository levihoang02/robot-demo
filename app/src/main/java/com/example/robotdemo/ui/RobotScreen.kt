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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.*
import com.example.robotdemo.adapter.VoiceNotifier
import com.example.robotdemo.domain.NavigationPoint
import com.example.robotdemo.domain.RobotStatus
import com.example.robotdemo.ui.theme.RobotDemoTheme
import androidx.compose.ui.tooling.preview.Preview

import com.example.robotdemo.UART.Protocol
import com.example.robotdemo.UART.SerialState

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
        navPoints = navPoints,
        onPointSelected = { point ->
            // Print coordinates in preview/log for demonstration
            viewModel.onPointSelected(point) 
        },
        onStopRobot = { viewModel.stopRobot() }
    )
}

@Composable
fun RobotScreenContent(
    robotStatus: RobotStatus,
    uartState: SerialState,
    navPoints: List<NavigationPoint>,
    onPointSelected: (NavigationPoint) -> Unit,
    onStopRobot: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedPoints by remember { mutableStateOf(listOf<NavigationPoint>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (robotStatus == RobotStatus.IDLE) Color(0xFFF8F9FA) else Color.Black)
    ) {
        UartStatusChip(
            state = uartState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        )
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

                    Text(
                        "SELECTION STACK",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF74777F)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // STACK AREA
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {

                        if (selectedPoints.isEmpty()) {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No points selected",
                                    color = Color.LightGray
                                )
                            }

                        } else {

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                selectedPoints.forEachIndexed { index, point ->

                                    Surface(
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {


                                            Spacer(modifier = Modifier.width(48.dp))

                                            Text(
                                                text = point.name,
                                                color = Color(0xFF1976D2),
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    selectedPoints = selectedPoints - point
                                                }
                                            ) {

                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFF1976D2),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // HOME BUTTON
                    Button(
                        onClick = {
                            val homePoint = NavigationPoint("home", "Home", Protocol.TARGET_HOME)
                            onPointSelected(homePoint)
                            selectedPoints= emptyList()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF263238),
                            contentColor = Color.White
                        )
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home"
                            )

                            Text(
                                "RETURN HOME",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // RIGHT: POINTS TABLE AND START
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "DESTINATIONS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF74777F)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(navPoints) { point ->
                            CompactDestinationCard(point.name) { 
                                if (!selectedPoints.contains(point)) {
                                    selectedPoints = selectedPoints + point 
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { 
                            if (selectedPoints.isNotEmpty()) {
                                // For now, we start the mission with the first point
                                onPointSelected(selectedPoints.first())
                                selectedPoints = emptyList() 
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        enabled = selectedPoints.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE0E0E0),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("START MISSION", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Full Screen Lottie for Active States
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showConfirmDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Status Text - Always in middle, behind Lottie (visible if Lottie not loaded)
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

        if (showConfirmDialog) {
            ElegantGlassDialog(
                onDismiss = { showConfirmDialog = false },
                onConfirm = { 
                    onStopRobot()
                    showConfirmDialog = false 
                }
            )
        }
    }
}

@Composable
fun CompactDestinationCard(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A1C1E),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun UartStatusChip(
    state: SerialState,
    modifier: Modifier = Modifier
) {

    val (text, color) =
        when (state) {

            is SerialState.Idle ->
                "UART Idle" to Color.Gray

            is SerialState.Connecting ->
                "Connecting..." to Color(0xFFFFA000)

            is SerialState.Connected ->
                "Connected" to Color(0xFF4CAF50)

            is SerialState.Reconnecting ->
                "Reconnecting..." to Color(0xFFFF9800)

            is SerialState.Error ->
                "UART Error" to Color.Red
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.7f)
    ) {

        Row(
            modifier = Modifier
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                ),
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

@Composable
fun RobotLottieAnimation(status: RobotStatus, modifier: Modifier = Modifier.size(350.dp)) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.example.robotdemo.R.raw.robot_cat))
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

@Composable
fun ElegantGlassDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 340.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Robot Control",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF74777F),
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Would you like to stop the current mission?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1A1C1E),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("Stop Robot", fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Continue Mission", color = Color(0xFF42474E))
                    }
                }
            }
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
            navPoints = listOf(
                NavigationPoint("1", "A", Protocol.TARGET_1), NavigationPoint("2", "B", Protocol.TARGET_2),
                NavigationPoint("3", "C", Protocol.TARGET_3)
            ),
            onPointSelected = {},
            onStopRobot = {}
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800, name = "Moving Lottie")
@Composable
fun PreviewMovingLottie() {
    RobotDemoTheme {
        RobotScreenContent(
            robotStatus = RobotStatus.MOVING,
            uartState = SerialState.Idle,
            navPoints = emptyList(),
            onPointSelected = {},
            onStopRobot = {}
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800, name = "Blocked Lottie")
@Composable
fun PreviewBlockedLottie() {
    RobotDemoTheme {
        RobotScreenContent(
            robotStatus = RobotStatus.BLOCKED,
            uartState = SerialState.Idle,
            navPoints = emptyList(),
            onPointSelected = {},
            onStopRobot = {}
        )
    }
}
