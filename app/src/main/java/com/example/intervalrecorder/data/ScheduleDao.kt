package com.example.intervalrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    // Insert a new schedule
    @Insert
    suspend fun insert(schedule: Schedule)

    // Get all schedules as a Flow
    @Query("SELECT * FROM schedules")
    fun getAllSchedules(): Flow<List<Schedule>>

    // Optional: Add methods for update or delete if needed
    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun delete(scheduleId: Int)
}

