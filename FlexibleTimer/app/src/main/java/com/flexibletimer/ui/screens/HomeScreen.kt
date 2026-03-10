package com.flexibletimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.flexibletimer.ui.components.PrimaryButton
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*

@Composable
fun HomeScreen(
    onSequentialClick: () -> Unit,
    onGroupClick: () -> Unit
) {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Flexible Timer",
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        item {
            PrimaryButton(
                text = "Sequential",
                onClick = onSequentialClick,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            PrimaryButton(
                text = "Group",
                onClick = onGroupClick
            )
        }
    }
}
