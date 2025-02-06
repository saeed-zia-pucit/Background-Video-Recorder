package com.example.intervalrecorder

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class VideoRecordingStopWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Trigger the stop of the ForegroundService
        val serviceIntent = Intent(applicationContext, VideoRecordingService::class.java).apply {
            putExtra("ACTION", "STOP_RECORDING")
        }
        ContextCompat.startForegroundService(applicationContext, serviceIntent)

        return Result.success()
    }
}
