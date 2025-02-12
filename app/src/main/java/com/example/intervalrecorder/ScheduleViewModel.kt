package com.example.intervalrecorder


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intervalrecorder.data.Schedule
import com.example.intervalrecorder.data.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor (private val repository: ScheduleRepository) : ViewModel() {

    // StateFlow to represent the current state of schedules
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

    init {
        // Collect data from the repository and update the StateFlow
        viewModelScope.launch {
            repository.getAllSchedules().collect { list ->
                _schedules.value = list
            }
        }
    }

    // Function to insert a schedule into the database
    fun insertSchedule(schedule: Schedule) {
        viewModelScope.launch {
            repository.insertSchedule(schedule)
        }
    }
    fun getScheduleById(id: Int): Schedule? {
        return _schedules.value.find { it.id == id }
    }

    // Optional: Function to delete a schedule
    fun deleteSchedule(scheduleId: Int) {
        viewModelScope.launch {
            repository.deleteSchedule(scheduleId)
        }
    }
}

