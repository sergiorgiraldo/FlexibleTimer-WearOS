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
import com.flexibletimer.ui.components.timerColors
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
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
    ) {
        when (state.timers.size) {
            1 -> GroupLayout1(state)
            2 -> GroupLayout2(state)
            3 -> GroupLayout3(state)
            else -> GroupLayout4(state)
        }
        Text(
            text = "Double-tap to stop",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun GroupLayout1(state: TimerRunState.GroupRunning) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        GroupTimerCell(
            label = state.timers[0].label.ifBlank { "T1" },
            remainingSeconds = state.remainingSeconds[0],
            color = timerColors[0],
            labelFontSize = 18.sp,
            timeFontSize = 36.sp,
        )
    }
}

@Composable
private fun GroupLayout2(state: TimerRunState.GroupRunning) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize().padding(start = 8.dp, top = 24.dp, end = 8.dp, bottom = 16.dp)
    ) {
        repeat(2) { idx ->
            GroupTimerCell(
                label = state.timers[idx].label.ifBlank { "T${idx + 1}" },
                remainingSeconds = state.remainingSeconds[idx],
                color = timerColors[idx],
                labelFontSize = 15.sp,
                timeFontSize = 26.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GroupLayout3(state: TimerRunState.GroupRunning) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize().padding(start = 8.dp, top = 22.dp, end = 8.dp, bottom = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            repeat(2) { idx ->
                GroupTimerCell(
                    label = state.timers[idx].label.ifBlank { "T${idx + 1}" },
                    remainingSeconds = state.remainingSeconds[idx],
                    color = timerColors[idx],
                    labelFontSize = 13.sp,
                    timeFontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        GroupTimerCell(
            label = state.timers[2].label.ifBlank { "T3" },
            remainingSeconds = state.remainingSeconds[2],
            color = timerColors[2],
            labelFontSize = 13.sp,
            timeFontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GroupLayout4(state: TimerRunState.GroupRunning) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize().padding(start = 8.dp, top = 22.dp, end = 8.dp, bottom = 12.dp)

    ) {
        repeat(2) { row ->
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                repeat(2) { col ->
                    val idx = row * 2 + col
                    GroupTimerCell(
                        label = state.timers[idx].label.ifBlank { "T${idx + 1}" },
                        remainingSeconds = state.remainingSeconds[idx],
                        color = timerColors[idx],
                        labelFontSize = 11.sp,
                        timeFontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FinishedScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "Done! ✓", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
