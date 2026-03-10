package com.flexibletimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.flexibletimer.data.model.SavedSequence
import com.flexibletimer.data.model.TimerEntry
import com.flexibletimer.presentation.group.GroupUiState
import com.flexibletimer.ui.components.PrimaryButton
import com.flexibletimer.ui.components.SavedItemChip
import com.flexibletimer.ui.components.Stepper

// ── Group menu ────────────────────────────────────────────────────────────────

@Composable
fun GroupMenuScreen(
    hasSaved: Boolean,
    onNewClick: () -> Unit,
    onSavedClick: () -> Unit
) {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Group", modifier = Modifier.padding(bottom = 16.dp)) }
        item {
            PrimaryButton(
                text = "New",
                onClick = onNewClick,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (hasSaved) {
            item {
                PrimaryButton(text = "Saved", onClick = onSavedClick)
            }
        }
    }
}

// ── Group new ─────────────────────────────────────────────────────────────────

@Composable
fun GroupNewScreen(
    uiState: GroupUiState,
    onTimerCountChange: (Int) -> Unit,
    onUpdateTimer: (Int, String?, Long?) -> Unit,
    onSave: (String) -> Unit,
    onStart: (List<TimerEntry>) -> Unit
) {
    var saveDialogVisible by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Stepper(
                value = uiState.timers.size,
                onValueChange = onTimerCountChange,
                range = 2..4,
                label = "Timers"
            )
        }
        uiState.timers.forEachIndexed { index, timer ->
            item {
                GroupTimerEntryEditor(
                    index = index,
                    timer = timer,
                    onLabelChange = { onUpdateTimer(index, it, null) },
                    onDurationChange = { onUpdateTimer(index, null, it) }
                )
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Button(onClick = { saveDialogVisible = true }) { Text("Save") }
                Button(onClick = { onStart(uiState.timers) }) { Text("Start") }
            }
        }
    }

    if (saveDialogVisible) {
        Dialog(showDialog = true, onDismissRequest = { saveDialogVisible = false }) {
            Alert(
                title = { Text("Save as") },
                negativeButton = { Button(onClick = { saveDialogVisible = false }) { Text("Cancel") } },
                positiveButton = {
                    Button(onClick = {
                        onSave(saveName)
                        saveDialogVisible = false
                    }) { Text("Save") }
                }
            ) {
                item {
                    TextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        placeholder = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── Group saved ───────────────────────────────────────────────────────────────

@Composable
fun GroupSavedScreen(
    groups: List<SavedSequence>,
    onSelect: (SavedSequence) -> Unit
) {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Saved Groups", modifier = Modifier.padding(bottom = 8.dp)) }
        groups.forEach { group ->
            item {
                SavedItemChip(
                    name = group.name,
                    onClick = { onSelect(group) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// ── Editor helper ─────────────────────────────────────────────────────────────

@Composable
private fun GroupTimerEntryEditor(
    index: Int,
    timer: TimerEntry,
    onLabelChange: (String) -> Unit,
    onDurationChange: (Long) -> Unit
) {
    var durationText by remember(timer.durationSeconds) {
        mutableStateOf(timer.durationSeconds.toString())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text("Timer ${index + 1}", style = MaterialTheme.typography.caption2)
        TextField(
            value = timer.label,
            onValueChange = onLabelChange,
            placeholder = { Text("Label") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = durationText,
            onValueChange = {
                durationText = it
                it.toLongOrNull()?.let { d -> onDurationChange(d) }
            },
            placeholder = { Text("Seconds") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
