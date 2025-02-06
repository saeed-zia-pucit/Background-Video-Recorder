package com.example.intervalrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.getStringExtra("ACTION") ?: return

        when (action) {
            "START_RECORDING" -> {
                Log.d("UserFlow", "START_RECORDING: ")
                val startIntent = Intent(context, VideoRecordingService::class.java).apply {
                    putExtra("ACTION", "START_RECORDING")
                }
                ContextCompat.startForegroundService(context, startIntent)
            }
            "STOP_RECORDING" -> {
                Log.d("UserFlow", "STOP_RECORDING: ")

                val stopIntent = Intent(context, VideoRecordingService::class.java).apply {
                    putExtra("ACTION", "STOP_RECORDING")
                }
                context.startService(stopIntent)
            }
        }
    }
}
