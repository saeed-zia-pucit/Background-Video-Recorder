package com.example.intervalrecorder

import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.intervalrecorder.Utils.cancelScheduledRecording
import com.example.intervalrecorder.Utils.parseDateTimeToMillis
import com.example.intervalrecorder.Utils.scheduleRecording
import com.example.intervalrecorder.data.Schedule
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRecordingScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add new Schedule",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary // Correct background color for Material 3
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        // Navigate back to the previous screen
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },

        content = { paddingValues ->
            FreshView(modifier = Modifier.padding(paddingValues),
                onSchedule = { startDate, startTime, stopTime, dayName ->

                    val startMillis = parseDateTimeToMillis(startDate, startTime)
                    val stopMillis = parseDateTimeToMillis(startDate, stopTime)
                    val startId = System.currentTimeMillis().toInt()
                    val stopId = startId + 1
                    if (startMillis != null && stopMillis != null && startMillis < stopMillis) {

                        scheduleRecording(context, startMillis, stopMillis, startId, stopId)

                        viewModel.insertSchedule(
                            Schedule(
                                0,
                                dayName,
                                startTime,
                                stopTime,
                                startId,
                                stopId,
                            )
                        )
                        Toast.makeText(context, "Recording Scheduled", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()

                    } else {
                        Toast.makeText(context, "Invalid date or time", Toast.LENGTH_SHORT).show()
                    }

                }, refreshAll = {
                    val stopIntent = Intent(context, VideoRecordingService::class.java).apply {
                        putExtra("ACTION", "STOP_RECORDING")
                    }
                    context.startForegroundService(stopIntent)

                    viewModel.schedules.value.forEach { schedule ->
                        cancelScheduledRecording(context, schedule.startId, schedule.stopId)
                        viewModel.deleteSchedule(schedule.id)
                    }
                    Toast.makeText(context, "Recording schedule canceled!", Toast.LENGTH_SHORT)
                        .show()
                    navController.popBackStack()

                }
            )
        })
}


@Composable
fun FreshView(
    modifier: Modifier,
    onSchedule: (startDate: String, startTime: String, stopDate: String, stopTime: String) -> Unit,
    refreshAll: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var stopTime by remember { mutableStateOf("") }
    var dayName by remember { mutableStateOf("") }

// Show DatePickerDialog
    val startDatePicker = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            // After date is selected, calculate the day of the week
            calendar.set(year, month, dayOfMonth)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Convert the day of week (1 = Sunday, 7 = Saturday) to the actual day name
            val daysOfWeek = arrayOf(
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
            )

            dayName = daysOfWeek[dayOfWeek - 1] // Adjust for 1-based indexing

            // Format the selected date (yyyy-MM-dd) and display the day of the week
            startDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            println("Selected date: $startDate, Day of the week: $dayName")
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val startTimePicker = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            startTime = String.format("%02d:%02d", hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    val stopTimePicker = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            stopTime = String.format("%02d:%02d", hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),

        false
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(30.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Button(onClick = { startDatePicker.show() }) {
            Text(if (startDate.isEmpty()) "Select Start Date" else "Start Date: $startDate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { startTimePicker.show() }) {
            Text(if (startTime.isEmpty()) "Select Start Time" else "Start Time: $startTime")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { stopTimePicker.show() }) {
            Text(if (stopTime.isEmpty()) "Select Stop Time" else "Stop Time: $stopTime")
        }

        Spacer(modifier = Modifier.height(25.dp))

        Button(onClick = {

            onSchedule(startDate, startTime, stopTime, dayName)
        }) {
            Text("Schedule")
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {

            refreshAll()
        }) {
            Text("Refresh All")
        }

    }
}






