package com.example.ha_bits.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_table WHERE userId = :userId ORDER BY id ASC")
    fun getAllHabits(userId: String): Flow<List<Habit>>

    @Query("SELECT * FROM habit_table WHERE userId = :userId ORDER BY id ASC")
    suspend fun getAllHabitsOnce(userId: String): List<Habit>

    @Update
    suspend fun update(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)
}