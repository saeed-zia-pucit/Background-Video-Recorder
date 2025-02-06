package com.example.intervalrecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.intervalrecorder.ui.theme.IntervalRecorderTheme
import com.google.firebase.BuildConfig
import pk.farimarwat.anrspy.agent.ANRSpyAgent
import pk.farimarwat.anrspy.agent.ANRSpyListener
import pk.farimarwat.anrspy.models.MethodModel

class MainActivity : ComponentActivity() {


    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Permissions denied: $deniedPermissions", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            }
        }

    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API 31) and above, check if the permission is granted
            if (!isExactAlarmPermissionGranted()) {
                // If permission is not granted, direct user to the system settings
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            } else {
                // Permission already granted, proceed with setting the alarm
               // scheduleRecording(context, startMillis, stopMillis)
            }
        } else {
            // For older Android versions, permission is not required
            //setExactAlarm()
        }
    }

    fun isExactAlarmPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check if the permission has been granted
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.SCHEDULE_EXACT_ALARM
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                false
            }
        } else {
            // On older Android versions, permission is not needed
            true
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


       // requestExactAlarmPermission()
        if (BuildConfig.DEBUG) {
            anrSpyAgent.start()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        //   requestPermissions()
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            var isShowPermissionDialog by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isShowPermissionDialog = !allPermissionsGranted()
            }
            IntervalRecorderTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    ScheduleRecordingScreen(modifier = Modifier.padding(innerPadding))
//                    PermissionHandler(
//                        snackbarHostState = snackbarHostState,
//                        onPermissionsGranted = { /* Do something when permissions are granted */ },
//                        onRequestPermissions = { requestPermissions() }
//                    )

                    if (isShowPermissionDialog) {

                        PermissionDialog({
                            isShowPermissionDialog = false
                            requestPermissions()
                        }, {
                            isShowPermissionDialog = false
                        })
                    }

                }
            }
        }
    }

    private var mCallback = object : ANRSpyListener {
        override fun onWait(ms: Long) {
            //Total blocking time of main thread.
            //Can be used for doing any action e.g. if blocked time is more than 5 seconds then
            //restart the app to avoid raising ANR message because it will lead to down rank your app.
        }

        override fun onAnrStackTrace(stackstrace: Array<StackTraceElement>) {
            //To  investigate ANR via stackstrace if occured.
            //This method is deprecated and will  be remove d in future
        }

        override fun onReportAvailable(methodList: List<MethodModel>) {
            //Get instant report about annotated methods if touches main thread more than target time
        }


        override fun onAnrDetected(
            details: String,
            stackTrace: Array<StackTraceElement>,
            packageMethods: List<String>?
        ) {
            Log.d("TAG", "onAnrDetected: ")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    val anrSpyAgent = ANRSpyAgent.Builder(this)
        .setTimeOut(2000)
        .setSpyListener(mCallback)
        .setThrowException(true)
        .enableReportAnnotatedMethods(true)
        .build()

    private fun requestPermissions() {
        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS.toTypedArray())
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}


//@Composable
//fun PermissionHandler(
//    snackbarHostState: SnackbarHostState,
//    onPermissionsGranted: () -> Unit,
//    onRequestPermissions: () -> Unit
//) {
//    val context = LocalContext.current
//    val coroutineScope = rememberCoroutineScope()
//
//    if (!allPermissionsGranted(context)) {
//        coroutineScope.launch {
//            val result = snackbarHostState.showSnackbar(
//                message = "Permissions are required for this feature.",
//                actionLabel = "Settings",
//                duration = SnackbarDuration.Indefinite
//            )
//            when (result) {
//                SnackbarResult.ActionPerformed -> {
//                    // Open app settings
//                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                        data = Uri.fromParts("package", context.packageName, null)
//                    }
//                    context.startActivity(intent)
//                }
//
//                SnackbarResult.Dismissed -> {
//                    Toast.makeText(context, "Permissions are still required", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }
//        }
//    } else {
//        onPermissionsGranted()
//        onRequestPermissions()
//    }
//}

@Composable
fun PermissionDialog(onAllowClick: () -> Unit, onCancelClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onCancelClick() },
        title = { Text("Permission Required") },
        text = { Text("This app needs permission to access your camera and microphone to record video.") },
        confirmButton = {
            TextButton(onClick = onAllowClick) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelClick) {
                Text("Cancel")
            }
        }
    )
}


