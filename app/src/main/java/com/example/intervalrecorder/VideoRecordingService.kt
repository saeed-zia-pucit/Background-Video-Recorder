package com.example.intervalrecorder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.RingtoneManager
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
import com.example.intervalrecorder.data.DataRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
@AndroidEntryPoint
class VideoRecordingService : LifecycleService() {
    @Inject
    lateinit var dataRepository: DataRepository
    private var recording: Recording? = null
    private var isRecording: Boolean = false
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    var isRecordingEnded: Boolean = false
    private var receiver: BroadcastReceiver? = null
    val filter = IntentFilter("com.example.snippets.ACTION_UPDATE_DATA")
    private val uiUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.hasExtra("ACTION_ID")) {
                startRecording(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            uiUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        CoroutineScope(Dispatchers.Main).launch {
            dataRepository.dataFlow.collect { data ->
                toggleRecording(data)
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.getStringExtra("ACTION") ?: ""
        var notificationTitle = "Video Recorder"
        val handler = Handler(Looper.getMainLooper())

        notificationTitle = when (action) {
            "START_RECORDING" -> {
                "Recording in progress"
            }

            "STOP_RECORDING" -> {
                "Recording stopped"
            }

            else -> {
                "Ready to Record"
            }
        }
        if (isRecording)
            notificationTitle = "Recording in Progress"
        startForeground(1, createNotification(notificationTitle))
        toggleRecording(action)
        return START_STICKY
    }

    private fun toggleRecording(action: String) {
        when (action) {
            "START_RECORDING" -> {
                cameraProviderFuture = ProcessCameraProvider.getInstance(this)

                startRecording(this)

                //    Toast.makeText(this, "Service Start Recording called", Toast.LENGTH_LONG).show()
                Log.d("UserFlow", "onStartCommand: START_RECORDING")

            }

            "STOP_RECORDING" -> {
                Log.d("UserFlow", "onStartCommand: STOP_RECORDING")
                //  Toast.makeText(this, "Service Stop Recording called", Toast.LENGTH_LONG).show()

                stopRecording()
            }
        }

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
            Log.e("UserFlow", "Permissions not granted for camera or audio recording")
            return
        }

        // Use a Coroutine to perform camera setup and media recording in the background
        CoroutineScope(Dispatchers.IO).launch {
            cameraProviderFuture.addListener(
                {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()  // Unbind any previous use cases

                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.SD))
                            .build()

                        val videoCapture = VideoCapture.withOutput(recorder)
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        // Bind camera and video capture
                        cameraProvider.bindToLifecycle(
                            this@VideoRecordingService,
                            cameraSelector,
                            videoCapture
                        )

                        // Prepare output file for recording
                        val contentValues = ContentValues().apply {
                            put(
                                MediaStore.MediaColumns.DISPLAY_NAME,
                                "VID_${
                                    SimpleDateFormat(
                                        "yyyyMMdd_HHmmss",
                                        Locale.US
                                    ).format(Date())
                                }"
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
                        recording = videoCapture.output.prepareRecording(
                            this@VideoRecordingService,
                            outputOptions
                        )
                            .withAudioEnabled()
                            .start(ContextCompat.getMainExecutor(this@VideoRecordingService)) { event ->
                                // Switch to the main thread for UI updates
                                when (event) {
                                    is VideoRecordEvent.Start -> {
                                        isRecording = true
                                        // Update UI on the main thread
//                                    launch(Dispatchers.Main) {
                                        Log.d("UserFlow", "Recording started")
                                        Toast.makeText(
                                            context,
                                            "Recording started",
                                            Toast.LENGTH_SHORT
                                        ).show()
//                                    }
                                    }

                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
//                                    launch(Dispatchers.Main) {
                                        if (event.hasError()) {
                                            Toast.makeText(
                                                context,
                                                "Error finalizing recording",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.e(
                                                "UserFlow",
                                                "Error finalizing recording: ${event.error}"
                                            )
                                            FirebaseCrashlytics.getInstance().recordException(
                                                event.cause
                                                    ?: Exception("CustomException:VideoRecordEvent.Finalize" + event.error)
                                            )
                                        } else {
                                            Log.d("UserFlow", "Recording finalized successfully")
                                        }
                                        cameraProvider.unbindAll()
                                    }
//                                }
                                }
                            }
                    } catch (e: Exception) {
//                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error Exception", Toast.LENGTH_SHORT).show()
                        Log.e("UserFlow", "Error starting recording: ${e.message}")
                        FirebaseCrashlytics.getInstance().recordException(e)
//                    }
                    }
                },
                ContextCompat.getMainExecutor(this@VideoRecordingService)
            ) // Running the listener on the main thread to handle camera provider future
        }
    }

    private fun stopRecording() {
        if (recording != null && isRecording) {
            try {
                recording?.stop() // Stop recording properly
                recording?.close() // Fully release MediaRecorder
                recording = null // Set to null to avoid memory leaks
                isRecording = false
                cameraProviderFuture.cancel(true)
                Log.d("UserFlow", "Recording stopped and released")
            } catch (e: Exception) {
                Log.e("UserFlow", "Error stopping MediaRecorder: ${e.message}")
            }
        } else {
            Log.d("UserFlow", "No active recording to stop")
        }
    }


    private fun createNotification(contentText: String): Notification {
        val channelId = "VideoRecordingChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Video Recording",
                NotificationManager.IMPORTANCE_HIGH // Set to HIGH for visibility
            ).apply {
//                enableVibration(true)
//                enableLights(true)
//                vibrationPattern = longArrayOf(0, 500, 1000) // Custom vibration
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Full-Screen Intent to make notification pop up
        val fullScreenIntent = Intent(this, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Video Recorder")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_assistant_navigation_24)  // Use your own icon
            .setPriority(NotificationCompat.PRIORITY_HIGH) // HIGH priority for Pre-Oreo
            .setDefaults(Notification.DEFAULT_ALL)  // Enable sound & vibration
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Play sound
            .setFullScreenIntent(fullScreenPendingIntent, true) // Force pop-up
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup camera and recording resources when the service is destroyed
        recording?.stop()
        recording?.close()
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
        this.unregisterReceiver(uiUpdateReceiver)
    }


}
