package com.example.intervalrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val day: String,
    val startTime: String,
    val stopTime: String,
    val startId:Int,
    val stopId:Int
)
