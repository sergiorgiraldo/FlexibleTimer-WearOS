package com.flexibletimer.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.flexibletimer.data.model.TimerRunState
import com.flexibletimer.ui.components.GroupTimerCell
import com.flexibletimer.ui.components.toHhMmSs

@Composable
fun RunningSequentialScreen(state: TimerRunState.SequentialRunning, onDoubleTap: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = state.label.ifBlank { "Timer ${state.currentIndex + 1}" },
                fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.remainingSeconds.toHhMmSs(),
                fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${state.currentIndex + 1} / ${state.timers.size}", fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Double-tap to stop", fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun RunningGroupScreen(state: TimerRunState.GroupRunning, onDoubleTap: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            val timerCount = state.timers.size
            val rows = (timerCount + 1) / 2
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..1) {
                        val idx = row * 2 + col
                        if (idx < timerCount) {
                            GroupTimerCell(
                                label = state.timers[idx].label.ifBlank { "T${idx + 1}" },
                                remainingSeconds = state.remainingSeconds[idx],
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Double-tap to stop", fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FinishedScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "Done! ✓", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
