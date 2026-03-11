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
        // #4: title shows mode name, not generic "Timers"
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
fun SequentialSavedScreen(sequences: List<SavedSequence>, onSelect: (SavedSequence) -> Unit, onDelete: (SavedSequence) -> Unit) {
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
    // Convert stored seconds → HH:MM:SS for display/editing
    var hhMmSsText by remember(timer.durationSeconds) {
        mutableStateOf(secondsToHhMmSs(timer.durationSeconds))
    }

    // #2: center the whole editor card
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text("Timer ${index + 1}", style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        // #3: each field wrapped in SwipeDismissableBox substitute — use a
        //     non-swipeable container so the outer swipe-to-go-back still works.
        //     BasicTextField consumes horizontal drag; we limit its width so
        //     edges remain free for the system swipe gesture.
        WearTextField(
            value = timer.label,
            onValueChange = onLabelChange,
            placeholder = "Label"
        )
        Spacer(Modifier.height(4.dp))
        WearTextField(
            value = hhMmSsText,
            onValueChange = { raw ->
                hhMmSsText = raw
                hhMmSsToSeconds(raw)?.let { onDurationChange(it) }
            },
            placeholder = "HH:MM:SS",
            keyboardType = KeyboardType.Number
        )
    }
}

// ── WearTextField ─────────────────────────────────────────────────────────────
// #3: width capped at 75% so left/right edges stay free for swipe-to-dismiss

@Composable
internal fun WearTextField(
    value: String, onValueChange: (String) -> Unit,
    placeholder: String, keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth(0.75f)   // leave edges free for swipe-dismiss
            .padding(2.dp)
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

/** Accepts "HH:MM:SS", "MM:SS", or plain seconds. Returns null if unparseable. */
internal fun hhMmSsToSeconds(input: String): Long? {
    val parts = input.trim().split(":")
    return when (parts.size) {
        3 -> {
            val h = parts[0].toLongOrNull() ?: return null
            val m = parts[1].toLongOrNull() ?: return null
            val s = parts[2].toLongOrNull() ?: return null
            h * 3600 + m * 60 + s
        }
        2 -> {
            val m = parts[0].toLongOrNull() ?: return null
            val s = parts[1].toLongOrNull() ?: return null
            m * 60 + s
        }
        1 -> parts[0].toLongOrNull()
        else -> null
    }
}
