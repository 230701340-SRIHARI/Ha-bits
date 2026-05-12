package com.example.habits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_table")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val streak: Int = 0,
    val lastCompleted: Long = 0L,
    val frequency: String = "Daily", // Daily, Weekly, Monthly
    val reminderTime: String = "12:00"
)