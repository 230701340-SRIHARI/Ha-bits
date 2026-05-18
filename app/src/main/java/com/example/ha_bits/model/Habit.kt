package com.example.ha_bits.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.PropertyName

@Entity(tableName = "habit_table")
data class Habit(
    @PrimaryKey(autoGenerate = true) 
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: Int = 0,
    
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",
    
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("streak")
    @set:PropertyName("streak")
    var streak: Int = 0,
    
    @get:PropertyName("lastCompleted")
    @set:PropertyName("lastCompleted")
    var lastCompleted: Long = 0L,
    
    @get:PropertyName("isCustom")
    @set:PropertyName("isCustom")
    var isCustom: Boolean = false,
    
    @get:PropertyName("isBroken")
    @set:PropertyName("isBroken")
    var isBroken: Boolean = false,
    
    @get:PropertyName("reminderHour")
    @set:PropertyName("reminderHour")
    var reminderHour: Int = 12,
    
    @get:PropertyName("reminderMinute")
    @set:PropertyName("reminderMinute")
    var reminderMinute: Int = 0,
    
    @get:PropertyName("isOneTime")
    @set:PropertyName("isOneTime")
    var isOneTime: Boolean = false,
    
    @get:PropertyName("isArchived")
    @set:PropertyName("isArchived")
    var isArchived: Boolean = false
)
