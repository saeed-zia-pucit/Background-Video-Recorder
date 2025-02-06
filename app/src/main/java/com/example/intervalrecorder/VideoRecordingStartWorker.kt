package com.example.intervalrecorder

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class VideoRecordingStartWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Trigger the start of the ForegroundService

        val serviceIntent = Intent(applicationContext, VideoRecordingService::class.java).apply {
            putExtra("ACTION", "START_RECORDING")
        }
        ContextCompat.startForegroundService(applicationContext, serviceIntent)

        return Result.success()
    }
}
