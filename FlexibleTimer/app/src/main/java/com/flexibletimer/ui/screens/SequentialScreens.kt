package com.flexibletimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.presentation.sequential.SequentialUiState
import com.flexibletimer.ui.components.PrimaryButton
import com.flexibletimer.ui.components.SavedItemChip
import com.flexibletimer.ui.components.Stepper

// ── Sequential menu ───────────────────────────────────────────────────────────

@Composable
fun SequentialMenuScreen(hasSaved: Boolean, onNewClick: () -> Unit, onSavedClick: () -> Unit) {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Sequential", modifier = Modifier.padding(bottom = 16.dp)) }
        item { PrimaryButton(text = "New", onClick = onNewClick, modifier = Modifier.padding(bottom = 8.dp)) }
        if (hasSaved) {
            item { PrimaryButton(text = "Saved", onClick = onSavedClick) }
        }
    }
}

// ── Sequential new ────────────────────────────────────────────────────────────

@Composable
fun SequentialNewScreen(
    uiState: SequentialUiState,
    onTimerCountChange: (Int) -> Unit,
    onUpdateTimer: (Int, String?, Long?) -> Unit,
    onSave: (String) -> Unit,
    onStart: (List<TimerEntry>) -> Unit
) {
    var showSaveScreen by remember { mutableStateOf(false) }

    if (showSaveScreen) {
        SaveNameScreen(
            onConfirm = { name -> onSave(name); showSaveScreen = false },
            onCancel = { showSaveScreen = false }
        )
        return
    }

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Sequential", modifier = Modifier.padding(bottom = 8.dp)) }
        item {
            Stepper(value = uiState.timers.size, onValueChange = onTimerCountChange, range = 1..10, label = "Count")
        }
        uiState.timers.forEachIndexed { index, timer ->
            item {
                TimerEntryEditor(
                    index = index, timer = timer,
                    onLabelChange = { onUpdateTimer(index, it, null) },
                    onDurationChange = { onUpdateTimer(index, null, it) }
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { showSaveScreen = true }) { Text("Save") }
                Button(onClick = { onStart(uiState.timers) }) { Text("Start") }
            }
        }
    }
}

// ── Sequential saved ──────────────────────────────────────────────────────────

@Composable
fun SequentialSavedScreen(
    sequences: List<SavedSequence>,
    onSelect: (SavedSequence) -> Unit,
    onDelete: (SavedSequence) -> Unit
) {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Saved", modifier = Modifier.padding(bottom = 8.dp)) }
        sequences.forEach { seq ->
            item { SavedItemChip(name = seq.name, onClick = { onSelect(seq) }, onDelete = { onDelete(seq) }, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

// ── Save name screen ──────────────────────────────────────────────────────────

@Composable
fun SaveNameScreen(onConfirm: (String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Save as", modifier = Modifier.padding(bottom = 8.dp)) }
        item { WearTextField(value = name, onValueChange = { name = it }, placeholder = "Name") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onCancel) { Text("Cancel") }
                Button(onClick = { onConfirm(name) }) { Text("Save") }
            }
        }
    }
}

// ── Timer entry editor ────────────────────────────────────────────────────────

@Composable
internal fun TimerEntryEditor(
    index: Int, timer: TimerEntry,
    onLabelChange: (String) -> Unit,
    onDurationChange: (Long) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text("Timer ${index + 1}", style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        WearTextField(value = timer.label, onValueChange = onLabelChange, placeholder = "Label")
        Spacer(Modifier.height(4.dp))
        // Calculator-style time input — always valid HH:MM:SS
        TimeInput(
            durationSeconds = timer.durationSeconds,
            onDurationChange = onDurationChange
        )
    }
}

// ── Calculator-style time input ───────────────────────────────────────────────
//
// Stores 6 digits (HHMMSS). Each new digit shifts in from the right:
//   Start:  [0,0,0,0,0,0] → "00:00:00"
//   Press 1:[0,0,0,0,0,1] → "00:00:01"
//   Press 2:[0,0,0,0,1,2] → "00:00:12"
//   Press 3:[0,0,0,1,2,3] → "00:01:23"
// Backspace shifts right, dropping the last digit.

@Composable
internal fun TimeInput(durationSeconds: Long, onDurationChange: (Long) -> Unit) {
    // Decompose stored seconds into 6 digits [H1,H2,M1,M2,S1,S2]
    var digits by remember(durationSeconds) {
        val h = (durationSeconds / 3600).coerceAtMost(99)
        val m = ((durationSeconds % 3600) / 60).coerceAtMost(59)
        val s = (durationSeconds % 60).coerceAtMost(59)
        mutableStateOf(
            intArrayOf(
                (h / 10).toInt(), (h % 10).toInt(),
                (m / 10).toInt(), (m % 10).toInt(),
                (s / 10).toInt(), (s % 10).toInt()
            )
        )
    }

    fun formatted() = "%d%d:%d%d:%d%d".format(
        digits[0], digits[1], digits[2], digits[3], digits[4], digits[5]
    )

    fun pushDigit(d: Int) {
        val new = intArrayOf(digits[1], digits[2], digits[3], digits[4], digits[5], d)
        digits = new
        val h = new[0] * 10 + new[1]
        val m = new[2] * 10 + new[3]
        val s = new[4] * 10 + new[5]
        onDurationChange(h * 3600L + m * 60L + s)
    }

    fun backspace() {
        val new = intArrayOf(0, digits[0], digits[1], digits[2], digits[3], digits[4])
        digits = new
        val h = new[0] * 10 + new[1]
        val m = new[2] * 10 + new[3]
        val s = new[4] * 10 + new[5]
        onDurationChange(h * 3600L + m * 60L + s)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Display
        Text(
            text = formatted(),
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Digit rows: 1-2-3 / 4-5-6 / 7-8-9 / ⌫-0
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { digit ->
                    Button(
                        onClick = { pushDigit(digit) },
                        modifier = Modifier.size(36.dp)
                    ) { Text(digit.toString(), fontSize = 13.sp) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        // Bottom row: backspace and 0
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = { backspace() },
                modifier = Modifier.size(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF444444))
            ) { Text("⌫", fontSize = 13.sp) }
            Button(
                onClick = { pushDigit(0) },
                modifier = Modifier.size(36.dp)
            ) { Text("0", fontSize = 13.sp) }
        }
    }
}

// ── WearTextField ─────────────────────────────────────────────────────────────

@Composable
internal fun WearTextField(
    value: String, onValueChange: (String) -> Unit,
    placeholder: String, keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth(0.75f).padding(2.dp)
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        BasicTextField(
            value = value, onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

// ── HH:MM:SS helpers ──────────────────────────────────────────────────────────

internal fun secondsToHhMmSs(total: Long): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
