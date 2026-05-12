package com.example.ha_bits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_table")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String,
    val streak: Int = 0,
    val lastCompleted: Long = 0L,
    val isCustom: Boolean = false,
    val isBroken: Boolean = false,
    val reminderHour: Int = 12,
    val reminderMinute: Int = 0,
    val isOneTime: Boolean = false,
    val isArchived: Boolean = false
)