package com.example.intervalrecorder.data
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val scheduleDao: ScheduleDao) {

    // Insert a new schedule into the database
    suspend fun insertSchedule(schedule: Schedule) {
        scheduleDao.insert(schedule)
    }
//    fun getScheduleById(scheduleId: Int): Flow<Schedule?> {
//        return scheduleDao.getScheduleById(scheduleId)
//    }

    // Get all schedules from the database as Flow
    fun getAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedules()
    }

    // Optional: Add methods for delete or update if needed
    suspend fun deleteSchedule(scheduleId: Int) {
        scheduleDao.delete(scheduleId)
    }
}
