package com.example.intervalrecorder

import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.intervalrecorder.Utils.cancelScheduledRecording
import com.example.intervalrecorder.Utils.clearScheduledTimes
import com.example.intervalrecorder.Utils.getScheduledTimes
import com.example.intervalrecorder.Utils.isRecordingScheduled
import com.example.intervalrecorder.Utils.parseDateTimeToMillis
import com.example.intervalrecorder.Utils.saveScheduledTimes
import com.example.intervalrecorder.Utils.scheduleRecording
import com.example.intervalrecorder.Utils.scheduleRecordingUsingWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRecordingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isScheduled by remember { mutableStateOf(isRecordingScheduled(context)) }
    val (scheduledStart, scheduledStop) = getScheduledTimes(context)

    if ( scheduledStart != null && scheduledStop != null) {
        // Show Scheduled View
        ScheduledView(
            scheduledStart = scheduledStart,
            scheduledStop = scheduledStop,
            onCancel = {
                val stopIntent = Intent(context, VideoRecordingService::class.java).apply {
                    putExtra("ACTION", "STOP_RECORDING")
                }
                context.startForegroundService(stopIntent)

               // cancelScheduledRecording(context)
                clearScheduledTimes(context)
                Toast.makeText(context, "Recording schedule canceled!", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        // Show Fresh View
        FreshView(
            onSchedule = { startTime, stopTime ->

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        // Save scheduled times (move disk I/O off the main thread)
                        saveScheduledTimes(context, startTime, stopTime)

                        // Schedule alarms
                       // scheduleRecording(context, startTime, stopTime)
                    }

                    // Start the foreground service
                    val stopIntent = Intent(context, VideoRecordingService::class.java).apply {
                        putExtra("startTime", startTime)
                        putExtra("endTime", stopTime)
                        putExtra("ACTION", "START_RECORDING")

                    }
                    context.startForegroundService(stopIntent)
                }
            }, onCancel = {
                val stopIntent = Intent(context, VideoRecordingService::class.java).apply {
                    putExtra("ACTION", "STOP_RECORDING")
                }
                context.startForegroundService(stopIntent)

               // cancelScheduledRecording(context)
                clearScheduledTimes(context)
                Toast.makeText(context, "Recording schedule canceled!", Toast.LENGTH_SHORT).show()

            }
        )
    }
}

@Composable
fun ScheduledView(scheduledStart: Long, scheduledStop: Long, onCancel: () -> Unit) {
    val startFormatted = remember(scheduledStart) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(scheduledStart))
    }
    val stopFormatted = remember(scheduledStop) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(scheduledStop))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Scheduled Recording", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Start Time: $startFormatted")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Stop Time: $stopFormatted")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onCancel) {
            Text("Cancel Scheduled Recording")
        }
    }
}

@Composable
fun FreshView(onSchedule: (startTime: Long, stopTime: Long,) -> Unit,onCancel: () -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var stopDate by remember { mutableStateOf("") }
    var stopTime by remember { mutableStateOf("") }

    val startDatePicker = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            startDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val stopDatePicker = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            stopDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Schedule Video Recording", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { startDatePicker.show() }) {
            Text(if (startDate.isEmpty()) "Select Start Date" else "Start Date: $startDate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { startTimePicker.show() }) {
            Text(if (startTime.isEmpty()) "Select Start Time" else "Start Time: $startTime")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { stopDatePicker.show() }) {
            Text(if (stopDate.isEmpty()) "Select Stop Date" else "Stop Date: $stopDate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { stopTimePicker.show() }) {
            Text(if (stopTime.isEmpty()) "Select Stop Time" else "Stop Time: $stopTime")
        }

        Spacer(modifier = Modifier.height(25.dp))

        Button(onClick = {
            val startMillis = parseDateTimeToMillis(startDate, startTime)
            val stopMillis = parseDateTimeToMillis(stopDate, stopTime)

            if (startMillis != null && stopMillis != null && startMillis < stopMillis) {
                onSchedule(startMillis, stopMillis)
            } else {
                Toast.makeText(context, "Invalid date or time", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Schedule")
        }

        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = {

        }) {
            Text("Refresh All")
        }
    }
}






