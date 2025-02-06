package com.example.intervalrecorder

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivities
import androidx.core.content.ContextCompat.startActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
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


    suspend fun scheduleRecording(context: Context, startMillis: Long, stopMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule Start Recording
        val startIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ACTION", "START_RECORDING")
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startMillis,
            startPendingIntent
        )

        // Schedule Stop Recording
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ACTION", "STOP_RECORDING")
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
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

    fun cancelScheduledRecording(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ACTION", "START_RECORDING")
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(startPendingIntent)

        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ACTION", "STOP_RECORDING")
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
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