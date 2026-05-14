package com.example.test.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.test.domain.NavigationPoint
import com.example.test.qna.VoiceState

@Composable
fun DestinationsPanel(
    navPoints: List<NavigationPoint>,
    selectedPoints: List<NavigationPoint>,
    voiceState: VoiceState,
    onPointClick: (NavigationPoint) -> Unit,
    onStartMission: () -> Unit,
    onMicClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DESTINATIONS",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF74777F)
            )
            
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
                    if (!selectedPoints.contains(point)) {
                        onPointClick(point)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onStartMission,
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
