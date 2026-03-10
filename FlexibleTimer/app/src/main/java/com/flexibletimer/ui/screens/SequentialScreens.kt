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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.*

import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.presentation.sequential.SequentialUiState
import com.flexibletimer.ui.components.PrimaryButton
import com.flexibletimer.ui.components.SavedItemChip
import com.flexibletimer.ui.components.Stepper

// ── Sequential menu ───────────────────────────────────────────────────────────

@Composable
fun SequentialMenuScreen(
    hasSaved: Boolean,
    onNewClick: () -> Unit,
    onSavedClick: () -> Unit
) {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Sequential", modifier = Modifier.padding(bottom = 16.dp)) }
        item {
            PrimaryButton(text = "New", onClick = onNewClick, modifier = Modifier.padding(bottom = 8.dp))
        }
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
        item {
            Stepper(value = uiState.timers.size, onValueChange = onTimerCountChange, range = 1..10, label = "Timers")
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
fun SequentialSavedScreen(sequences: List<SavedSequence>, onSelect: (SavedSequence) -> Unit) {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Saved", modifier = Modifier.padding(bottom = 8.dp)) }
        sequences.forEach { seq ->
            item { SavedItemChip(name = seq.name, onClick = { onSelect(seq) }, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

// ── Save name screen (replaces dialog) ───────────────────────────────────────

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

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
internal fun TimerEntryEditor(
    index: Int, timer: TimerEntry,
    onLabelChange: (String) -> Unit,
    onDurationChange: (Long) -> Unit
) {
    var durationText by remember(timer.durationSeconds) { mutableStateOf(timer.durationSeconds.toString()) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Timer ${index + 1}", style = MaterialTheme.typography.caption2)
        Spacer(Modifier.height(4.dp))
        WearTextField(value = timer.label, onValueChange = onLabelChange, placeholder = "Label")
        Spacer(Modifier.height(4.dp))
        WearTextField(
            value = durationText,
            onValueChange = { durationText = it; it.toLongOrNull()?.let { d -> onDurationChange(d) } },
            placeholder = "Seconds",
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
internal fun WearTextField(
    value: String, onValueChange: (String) -> Unit,
    placeholder: String, keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(modifier = Modifier.fillMaxWidth().padding(2.dp)) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = Color.Gray, fontSize = 13.sp)
        }
        BasicTextField(
            value = value, onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
    }
}
