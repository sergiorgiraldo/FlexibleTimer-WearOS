package com.flexibletimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth(0.8f).height(48.dp)) {
        Text(text = text, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun SavedItemChip(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chip(onClick = onClick, label = { Text(text = name, maxLines = 1) }, modifier = modifier.fillMaxWidth())
}

/** Formats seconds as HH:MM:SS */
fun Long.toHhMmSs(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
fun GroupTimerCell(label: String, remainingSeconds: Long, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(4.dp)) {
        Text(text = label.ifBlank { "Timer" }, fontSize = 10.sp, maxLines = 1)
        Text(text = remainingSeconds.toHhMmSs(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Stepper(value: Int, onValueChange: (Int) -> Unit, range: IntRange, label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(label, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (value > range.first) onValueChange(value - 1) }, modifier = Modifier.size(36.dp)) { Text("-") }
            Text(text = value.toString(), modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
            Button(onClick = { if (value < range.last) onValueChange(value + 1) }, modifier = Modifier.size(36.dp)) { Text("+") }
        }
    }
}
