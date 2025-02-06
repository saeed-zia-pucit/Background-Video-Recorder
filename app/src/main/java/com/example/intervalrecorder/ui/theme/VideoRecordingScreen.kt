package com.example.intervalrecorder.ui.theme

import android.Manifest
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.intervalrecorder.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VideoRecorderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recording: Recording? = null // Track the current recording session
    val coroutineScope = rememberCoroutineScope()

    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (!result.values.all { it }) {
                Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

        Button(onClick = {
            coroutineScope.launch(Dispatchers.Main) {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()

                val videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.bindToLifecycle(
                    context as ComponentActivity,
                    cameraSelector,
                    videoCapture
                )

                if (!isRecording) {
                    // Prepare video output options
                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
                        )
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    }

                    val outputOptions = MediaStoreOutputOptions.Builder(
                        context.contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    )
                        .setContentValues(contentValues)
                        .build()

                    // Start recording
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                    }
                    recording = videoCapture.output.prepareRecording(context, outputOptions)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(context)) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> isRecording = true
                                is VideoRecordEvent.Finalize -> {
                                    if (event.hasError()) {
                                        Log.e("VideoRecording", "Error: ${event.error}")
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Video saved successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    isRecording = false
                                }
                            }
                        }
                } else {
                    // Stop recording
                    recording?.stop()
                    recording?.close()
                    isRecording = false
                }
            }
        }) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}

@Preview
@Composable
fun VideoPreview() {
    VideoRecorderScreen(Modifier)
}
