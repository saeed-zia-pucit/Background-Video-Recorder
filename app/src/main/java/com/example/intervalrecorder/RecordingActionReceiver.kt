package com.example.intervalrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.intervalrecorder.data.DataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dataRepository: DataRepository
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.getStringExtra("ACTION") ?: return

        when (action) {
            "START_RECORDING" -> {
                Log.d("UserFlow", "AlarmReceiver_START_RECORDING: ")
                val startIntent = Intent(context, VideoRecordingService::class.java).apply {
                    putExtra("ACTION", "START_RECORDING")
                }

                CoroutineScope(Dispatchers.IO).launch {
                    dataRepository.emitData("START_RECORDING")
                }
            }

            "STOP_RECORDING" -> {
                Log.d("UserFlow", "AlarmReceiver_STOP_RECORDING: ")
                CoroutineScope(Dispatchers.IO).launch {
                    dataRepository.emitData("STOP_RECORDING")
                }

            }
        }
    }

}
