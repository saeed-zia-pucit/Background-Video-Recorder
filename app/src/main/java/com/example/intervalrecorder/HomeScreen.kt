package com.example.intervalrecorder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.intervalrecorder.Utils.cancelScheduledRecording
import com.example.intervalrecorder.data.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: ScheduleViewModel = hiltViewModel()
    val schedules by viewModel.schedules.collectAsState()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Schedules",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary // Correct background color for Material 3
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("scheduleRecording") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(schedules.size) { index ->
                        val item = schedules[index]
                        ScheduleItemView(item = item, onDelete = {
                            val schedule = viewModel.getScheduleById(item.id)
                            cancelScheduledRecording(
                                context = context,
                                startId = schedule?.startId ?: 0,
                                stopId = schedule?.stopId ?: 1
                            )
                            viewModel.deleteSchedule(item.id)

                        }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ScheduleItemView(item: Schedule, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp), // Rounded corners
        elevation = CardDefaults.cardElevation(4.dp) // Elevation for shadow effect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.startTime,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = item.stopTime,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewScheduleScreen() {
    HomeScreen(navController = NavController(LocalContext.current))
}
