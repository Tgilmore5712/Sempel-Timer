package com.pmc.timer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: TimerViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val longestSeconds = if (state.items.isEmpty()) 0 else (state.items.maxOf { it.minutes } * 60).toInt()
    val remaining = (longestSeconds - state.elapsed).coerceAtLeast(0)
    val progress = if (longestSeconds > 0) (state.elapsed.toFloat() / longestSeconds) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seafood Boil Timer", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Add everything with its total cook time. The longest item starts first, then the timer tells you exactly when to drop in each shorter-cooking item so everything finishes together.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // Main Timer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total boil time", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            formatTime(remaining),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.toggleTimer() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(if (state.running) Icons.Default.Close else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (state.running) "Pause" else "Start")
                            }
                            OutlinedButton(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Reset")
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color.Black,
                            trackColor = Color.LightGray
                        )

                        if (state.alertMessage.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFFA000))
                                    Spacer(Modifier.width(12.dp))
                                    Text(state.alertMessage, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Food Items List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Food items", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.resetDefaults() }) {
                            Text("Reset defaults")
                        }
                        TextButton(onClick = { viewModel.addItem() }) {
                            Text("Add")
                        }
                    }
                }
            }

            items(state.items, key = { it.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { viewModel.updateItem(item.id, it, item.minutes) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = if (item.minutes == 0.0) "" else item.minutes.toString(),
                        onValueChange = { 
                            val mins = it.toDoubleOrNull() ?: 0.0
                            viewModel.updateItem(item.id, item.name, mins)
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("min", fontSize = 10.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = { viewModel.removeItem(item.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                    }
                }
            }

            // Schedule Section
            item {
                Text("Drop-in schedule", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            }

            val schedule = state.items.sortedByDescending { it.minutes }.map { item ->
                val dropAt = (longestSeconds - (item.minutes * 60)).toInt()
                item to dropAt
            }.sortedBy { it.second }

            items(schedule) { (item, dropAt) ->
                val hasDropped = state.elapsed >= dropAt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasDropped) Color(0xFFE8F5E9) else Color.White
                    ),
                    border = if (!hasDropped) ButtonDefaults.outlinedButtonBorder else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text("Cook time: ${item.minutes} min", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Drop at", fontSize = 12.sp, color = Color.Gray)
                            Text(formatTime(dropAt), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
