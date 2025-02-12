package com.example.intervalrecorder.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class DataRepository {

    private val _dataFlow = MutableSharedFlow<String>()  // SharedFlow to emit data
    val dataFlow: SharedFlow<String> = _dataFlow

    // Method to emit data to the flow
    suspend fun emitData(data: String) {
        _dataFlow.emit(data)
    }
}
