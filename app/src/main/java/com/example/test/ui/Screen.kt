package com.example.test.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test.UART.Protocol
import com.example.test.UART.SerialState
import com.example.test.adapter.VoiceNotifier
import com.example.test.domain.NavigationPoint
import com.example.test.domain.RobotStatus
import com.example.test.qna.QnARepository
import com.example.test.qna.VoiceState
import com.example.test.ui.components.*
import com.example.test.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.statusBarsPadding

enum class ScreenMode {
    FULL,
    PRIVATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    viewModel: ViewModel = viewModel()
) {

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(Unit) {

        if (!isPreview) {
            QnARepository.initialize(context)
        }
    }


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

    ScreenContent(
        robotStatus = robotStatus,
        uartState = uartState,
        voiceState = voiceState,
        navPoints = navPoints,
        onPointSelected = { viewModel.onPointSelected(it) },
        onStopRobot = { viewModel.stopRobot() },
        onMicClicked = {

            Log.d("QNA_DEBUG", "Mic button clicked")
            Log.d("QNA_DEBUG", "Current state = $voiceState")

            if (voiceState is VoiceState.Listening) {

                Log.d("QNA_DEBUG", "Stopping listening")

                QnARepository.stopListening()

            } else {

                Log.d("QNA_DEBUG", "Starting listening")

                QnARepository.startListening()
            }
        },
        onCloseAssistant = {
            Log.d("QNA_DEBUG", "Stop Assistant")
            QnARepository.closeAssistant()
        }
    )
}

@Composable
fun ScreenContent(
    robotStatus: RobotStatus,
    uartState: SerialState,
    voiceState: VoiceState,
    navPoints: List<NavigationPoint>,
    onPointSelected: (NavigationPoint) -> Unit,
    onStopRobot: () -> Unit,
    onMicClicked: () -> Unit,
    onCloseAssistant: () -> Unit
) {

    var showConfirmDialog by remember {
        mutableStateOf(false)
    }

    var selectedPoints by remember {
        mutableStateOf(listOf<NavigationPoint>())
    }

    var screenMode by remember {
        mutableStateOf(ScreenMode.PRIVATE)
    }

    val scope = rememberCoroutineScope()

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (robotStatus == RobotStatus.IDLE)
                    Color(0xFFF8F9FA)
                else
                    Color.Black
            )
            .pointerInput(Unit) {

                detectTapGestures(

                    onTap = {

                        val now = System.currentTimeMillis()

                        /**
                         * Reset nếu tap quá chậm
                         */
                        if (now - lastTapTime > 1000) {
                            tapCount = 0
                        }

                        tapCount++

                        lastTapTime = now

                        if (tapCount >= 6) {

                            tapCount = 0

                            screenMode =
                                if (screenMode == ScreenMode.PRIVATE) {
                                    ScreenMode.FULL
                                } else {
                                    ScreenMode.PRIVATE
                                }
                        }
                    }
                )
            }
    ) {

        // =========================================================
        // PRIVATE MODE
        // =========================================================

        if (screenMode == ScreenMode.PRIVATE) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                FilledTonalButton(
                    onClick = onMicClicked,
                    modifier = Modifier
                        .width(240.dp)
                        .height(76.dp)
                ) {

                    Text(
                        text = "Open Assistant"
                    )
                }
            }
        }

        // =========================================================
        // FULL MODE
        // =========================================================

        else {

            if (robotStatus == RobotStatus.IDLE) {

                Row(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // LEFT PANEL

                    SelectionStack(
                        selectedPoints = selectedPoints,

                        onRemovePoint = {
                            selectedPoints = selectedPoints - it
                        },

                        onReturnHome = {

                            onPointSelected(
                                NavigationPoint(
                                    "home",
                                    "Home",
                                    Protocol.TARGET_HOME
                                )
                            )

                            selectedPoints = emptyList()
                        },

                        modifier = Modifier.weight(1f)
                    )

                    // RIGHT PANEL

                    DestinationsPanel(
                        navPoints = navPoints,

                        selectedPoints = selectedPoints,

                        voiceState = voiceState,

                        onPointClick = {
                            selectedPoints = selectedPoints + it
                        },

                        onStartMission = {

                            if (selectedPoints.isNotEmpty()) {

                                onPointSelected(
                                    selectedPoints.first()
                                )

                                selectedPoints = emptyList()
                            }
                        },

                        onMicClicked = onMicClicked,

                        modifier = Modifier.weight(1.2f)
                    )
                }

            } else {

                ActiveMissionScreen(
                    robotStatus = robotStatus,

                    onShowDialog = {
                        showConfirmDialog = true
                    }
                )
            }
        }

        // =========================================================
        // VOICE OVERLAY
        // =========================================================

        VoiceAssistantOverlay(
            voiceState = voiceState,
            isVisible = voiceState !is VoiceState.Idle,
            onClose = {
                QnARepository.closeAssistant()
            },
            onRetry = {
                QnARepository.startListening()
            }
        )

        // =========================================================
        // UART STATUS
        // =========================================================

        UartStatusChip(
            state = uartState,

            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(
                    start = 12.dp,
                    top = 8.dp
                )
        )

        // =========================================================
        // STOP DIALOG
        // =========================================================

        if (showConfirmDialog) {

            ElegantGlassDialog(

                onDismiss = {
                    showConfirmDialog = false
                },

                onConfirm = {

                    onStopRobot()

                    showConfirmDialog = false
                }
            )
        }
    }
}

@Preview(
    widthDp = 1280,
    heightDp = 800,
    name = "Modern Idle"
)
@Composable
fun PreviewModernIdle() {

    Theme {

        ScreenContent(

            robotStatus = RobotStatus.IDLE,

            uartState = SerialState.Idle,

            voiceState = VoiceState.Idle,

            navPoints = listOf(
                NavigationPoint(
                    "1",
                    "A",
                    Protocol.TARGET_1
                ),

                NavigationPoint(
                    "2",
                    "B",
                    Protocol.TARGET_2
                ),

                NavigationPoint(
                    "3",
                    "C",
                    Protocol.TARGET_3
                )
            ),

            onPointSelected = {},

            onStopRobot = {},

            onMicClicked = {},

            onCloseAssistant = {}
        )
    }
}

@Preview(
    widthDp = 1280,
    heightDp = 800,
    name = "QnA Speaking"
)
@Composable
fun PreviewQnASpeaking() {

    Theme {

        ScreenContent(

            robotStatus = RobotStatus.IDLE,

            uartState = SerialState.Connected,

            voiceState = VoiceState.Speaking(
                "Tôi là trợ lý của bạn, tôi có thể giúp gì cho bạn?"
            ),

            navPoints = emptyList(),

            onPointSelected = {},

            onStopRobot = {},

            onMicClicked = {},

            onCloseAssistant = {}
        )
    }
}