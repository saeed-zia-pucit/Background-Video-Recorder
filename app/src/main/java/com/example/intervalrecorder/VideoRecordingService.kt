package com.example.intervalrecorder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoRecordingService : LifecycleService() {

    private var recording: Recording? = null
    private var isRecording: Boolean = false
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    var isRecordingEnded: Boolean = false

    override fun onCreate() {
        super.onCreate()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    }

    fun startAndStopRecordingAtGivenTimes(startTime: Long, endTime: Long) {
        val handler = Handler(Looper.getMainLooper())

        // Calculate the delay for starting and stopping the recording
        val currentTime = System.currentTimeMillis()
        val startDelay = startTime - currentTime
        val endDelay = endTime - currentTime

        // Schedule the startRecording function
        handler.postDelayed({
            startRecording(this)
        }, startDelay)

        // Schedule the stopRecording function
        handler.postDelayed({
            stopRecording()
        }, endDelay)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.getStringExtra("ACTION") ?: ""
        var notificationTitle = "Recording scheduled"

        startForeground(1, createNotification(notificationTitle))

        when (action) {
            "START_RECORDING" -> {
                notificationTitle = "Recording in progress"
               // startRecording(this)
                Toast.makeText(this, "Please wait a moment", Toast.LENGTH_SHORT).show()

                Log.d("UserFlow", "onStartCommand: START_RECORDING")

                val startTime = intent?.getLongExtra("startTime",0L)
                val endTime = intent?.getLongExtra("endTime",0L)
                startAndStopRecordingAtGivenTimes(startTime?:0L,endTime?:0L)

            }

            "STOP_RECORDING" -> {
                Log.d("UserFlow", "onStartCommand: STOP_RECORDING")

                stopRecording()
            }
        }

        return START_STICKY
    }



    private fun startRecording(context: Context) {
        // Check necessary permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("VideoRecordingService", "Permissions not granted for camera or audio recording")
            return
        }

        // Use a Coroutine to perform camera setup and media recording in the background
        CoroutineScope(Dispatchers.IO).launch {
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()  // Unbind any previous use cases

                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.SD))
                        .build()

                    val videoCapture = VideoCapture.withOutput(recorder)
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Bind camera and video capture
                    cameraProvider.bindToLifecycle(this@VideoRecordingService, cameraSelector, videoCapture)

                    // Prepare output file for recording
                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
                        )
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    }

                    val outputOptions = MediaStoreOutputOptions.Builder(
                        contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    )
                        .setContentValues(contentValues)
                        .build()

                    // Start recording in the background
                    recording = videoCapture.output.prepareRecording(this@VideoRecordingService, outputOptions)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(this@VideoRecordingService)) { event ->
                            // Switch to the main thread for UI updates
                            when (event) {
                                is VideoRecordEvent.Start -> {
                                    isRecording = true
                                    // Update UI on the main thread
                                    launch(Dispatchers.Main) {
                                        Log.d("VideoRecordingService", "Recording started")
                                        Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                is VideoRecordEvent.Finalize -> {
                                    isRecording = false
                                    launch(Dispatchers.Main) {
                                        if (event.hasError()) {
                                            Toast.makeText(context, "Error finalizing recording", Toast.LENGTH_SHORT).show()
                                            Log.e("VideoRecordingService", "Error finalizing recording: ${event.error}")
                                            FirebaseCrashlytics.getInstance().recordException(
                                                event.cause ?: Exception("CustomException:VideoRecordEvent.Finalize" + event.error)
                                            )
                                        } else {
                                            Log.d("VideoRecordingService", "Recording finalized successfully")
                                        }
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error Exception", Toast.LENGTH_SHORT).show()
                        Log.e("VideoRecordingService", "Error starting recording: ${e.message}")
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
                }
            }, ContextCompat.getMainExecutor(this@VideoRecordingService)) // Running the listener on the main thread to handle camera provider future
        }
    }




    fun clearScheduledTimes(context: Context) {
        val sharedPreferences = context.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .remove("startTime")
            .remove("stopTime")
            .apply()
    }
    private fun stopRecording() {
        if (recording != null && isRecording) {
            recording?.stop()
            recording?.close()
            isRecording = false
            Log.d("VideoRecordingService", "Recording stopped")
        } else {
            Log.d("VideoRecordingService", "No active recording to stop")
        }

        clearScheduledTimes(this)
        // Stop the service and remove foreground notification
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(contentText: String): Notification {
        val channelId = "VideoRecordingChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Video Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Video Recorder")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_assistant_navigation_24)  // Use your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup camera and recording resources when the service is destroyed
        recording?.stop()
        recording?.close()
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    }
}
