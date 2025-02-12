package com.example.intervalrecorder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Utils {
    fun isRecordingScheduled(context: Context): Boolean {
        return isAlarmScheduled(context, 0) && isAlarmScheduled(context, 1)
    }

    fun isAlarmScheduled(context: Context, requestCode: Int): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }

    fun parseDateTimeToMillis(date: String, time: String): Long? {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateTime = formatter.parse("$date $time")
            dateTime?.time
        } catch (e: Exception) {
            null
        }
    }

    fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    fun scheduleRecordingUsingWorkManager(context: Context, startMillis: Long, stopMillis: Long) {
        // Calculate the delay from now to the start time
        val startDelay = startMillis - System.currentTimeMillis()
        val stopDelay = stopMillis - System.currentTimeMillis()

        // Ensure delays are not negative (start and stop times should be in the future)
        if (startDelay > 0) {
            // Create and enqueue the start recording work
            val startRecordingRequest: WorkRequest =
                OneTimeWorkRequestBuilder<VideoRecordingStartWorker>()
                    .setInitialDelay(startDelay, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context).enqueue(startRecordingRequest)
        } else {
            Log.e("scheduleRecording", "Start time must be in the future")
        }

        // Ensure delays are not negative (start and stop times should be in the future)
        if (stopDelay > 0) {
            // Create and enqueue the stop recording work
            val stopRecordingRequest: WorkRequest =
                OneTimeWorkRequestBuilder<VideoRecordingStopWorker>()
                    .setInitialDelay(stopDelay, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context).enqueue(stopRecordingRequest)
        } else {
            Log.e("scheduleRecording", "Stop time must be in the future")
        }
        Toast.makeText(context, "Recording Scheduled", Toast.LENGTH_SHORT).show()
    }


    fun scheduleRecording(
        context: Context,
        startMillis: Long,
        stopMillis: Long,
        startId: Int,
        stopId: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule Start Recording
        val startIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ACTION", "START_RECORDING")
            putExtra("id", startId)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            startId,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            startMillis,
            startPendingIntent
        )

        // Schedule Stop Recording
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ACTION", "STOP_RECORDING")
            putExtra("id", stopId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            stopId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            stopMillis,
            stopPendingIntent
        )

        Log.d("ScheduleRecording", "Recording scheduled from $startMillis to $stopMillis")
//        Toast.makeText(context, "Recording scheduled!", Toast.LENGTH_SHORT).show()
    }

    fun cancelScheduledRecording(context: Context, startId: Int, stopId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(context, AlarmReceiver::class.java)
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            startId,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(startPendingIntent)

        val stopIntent = Intent(context, AlarmReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            stopId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(stopPendingIntent)
    }


    fun clearScheduledTimes(context: Context) {
        val sharedPreferences = context.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .remove("startTime")
            .remove("stopTime")
            .apply()
    }

    fun getScheduledTimes(context: Context): Pair<Long?, Long?> {
        val sharedPreferences = context.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
        val startTime = sharedPreferences.getLong("startTime", -1L)
        val stopTime = sharedPreferences.getLong("stopTime", -1L)

        return if (startTime != -1L && stopTime != -1L) {
            Pair(startTime, stopTime)
        } else {
            Pair(null, null)
        }
    }

    suspend fun saveScheduledTimes(context: Context, startTime: Long, stopTime: Long) {

        val sharedPreferences =
            context.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putLong("startTime", startTime)
            .putLong("stopTime", stopTime)
            .apply()

    }

    fun allPermissionsGranted(context: Context): Boolean {
        val permissions = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
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

fun clearScheduledTimes(context: Context) {
    val sharedPreferences = context.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
    sharedPreferences.edit()
        .remove("startTime")
        .remove("stopTime")
        .apply()
}