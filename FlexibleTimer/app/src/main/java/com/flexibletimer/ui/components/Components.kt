package com.flexibletimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
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
fun SavedItemChip(name: String, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Chip(
            onClick = onClick,
            label = { Text(text = name, maxLines = 1) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        Button(
            onClick = onDelete,
            modifier = Modifier.size(36.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8B0000))
        ) {
            Text("✕", fontSize = 12.sp)
        }
    }
}

/** Formats seconds as HH:MM:SS */
fun Long.toHhMmSs(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

val timerColors = listOf(
    Color(0xFF4FC3F7), // light blue
    Color(0xFFA5D6A7), // light green
    Color(0xFFFFCC80), // light orange
    Color(0xFFCE93D8), // light purple
)

@Composable
fun GroupTimerCell(
    label: String,
    remainingSeconds: Long,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    labelFontSize: TextUnit = 13.sp,
    timeFontSize: TextUnit = 22.sp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(4.dp)) {
        Text(text = label.ifBlank { "Timer" }, fontSize = labelFontSize, maxLines = 1, color = color)
        Text(text = remainingSeconds.toHhMmSs(), fontSize = timeFontSize, fontWeight = FontWeight.Bold, color = color)
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
