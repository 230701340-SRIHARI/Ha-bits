package com.example.ha_bits.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.ha_bits.model.Habit
import com.example.ha_bits.model.HabitDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = HabitDatabase.getDatabase(application).habitDao()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().getReference("users")
    private val userId: String? = auth.currentUser?.uid

    val allHabits: LiveData<List<Habit>> = if (userId != null) {
        dao.getAllHabits(userId).asLiveData()
    } else {
        MutableLiveData(emptyList())
    }

    init {
        syncFromFirebase()
    }

    private fun syncFromFirebase() {
        val currentUserId = userId ?: return
        db.child(currentUserId).child("habits").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    val firebaseHabits = mutableListOf<Habit>()
                    for (habitSnapshot in snapshot.children) {
                        val habit = habitSnapshot.getValue(Habit::class.java)
                        if (habit != null) {
                            // Robust recovery of boolean flags to prevent sync issues
                            val archived = habitSnapshot.child("isArchived").getValue(Boolean::class.java) ?: habit.isArchived
                            val oneTime = habitSnapshot.child("isOneTime").getValue(Boolean::class.java) ?: habit.isOneTime
                            val broken = habitSnapshot.child("isBroken").getValue(Boolean::class.java) ?: habit.isBroken
                            val custom = habitSnapshot.child("isCustom").getValue(Boolean::class.java) ?: habit.isCustom
                            
                            firebaseHabits.add(habit.copy(
                                userId = currentUserId,
                                isArchived = archived,
                                isOneTime = oneTime,
                                isBroken = broken,
                                isCustom = custom
                            ))
                        }
                    }
                    
                    withContext(Dispatchers.IO) {
                        if (snapshot.exists()) {
                            val localHabits = dao.getAllHabitsOnce(currentUserId)
                            val firebaseIds = firebaseHabits.map { it.id }.toSet()
                            
                            localHabits.forEach { local ->
                                if (local.id !in firebaseIds) {
                                    dao.delete(local)
                                }
                            }
                            firebaseHabits.forEach { dao.insert(it) }
                        } else {
                            dao.clearUserHabits(currentUserId)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun completeHabit(habit: Habit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (isSameDay(habit.lastCompleted, now)) return@launch

            val newStreak = habit.streak + 1
            val updatedHabit = habit.copy(streak = newStreak, lastCompleted = now)
            
            updateHabit(updatedHabit)
        }
    }

    fun archiveHabit(habit: Habit) {
        viewModelScope.launch {
            // Always archive instead of deleting so it stays in history
            val updatedHabit = habit.copy(isArchived = true)
            updateHabit(updatedHabit)
        }
    }

    fun startFresh(habit: Habit) {
        viewModelScope.launch {
            val updatedHabit = habit.copy(
                streak = 1,
                lastCompleted = System.currentTimeMillis(),
                isBroken = false,
                isArchived = false
            )
            updateHabit(updatedHabit)
        }
    }

    private suspend fun updateHabit(habit: Habit) {
        dao.update(habit)
        userId?.let { uid ->
            db.child(uid).child("habits").child(habit.id.toString()).setValue(habit)
        }
    }

    fun addCustomHabit(name: String, reminderHour: Int, reminderMinute: Int, isOneTime: Boolean) {
        val currentUserId = userId ?: return
        viewModelScope.launch {
            val newHabit = Habit(
                userId = currentUserId, 
                name = name, 
                isCustom = true,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                isOneTime = isOneTime
            )
            val id = dao.insert(newHabit)
            val habitWithId = newHabit.copy(id = id.toInt())
            
            db.child(currentUserId).child("habits").child(habitWithId.id.toString()).setValue(habitWithId)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            dao.delete(habit)
            userId?.let { uid ->
                db.child(uid).child("habits").child(habit.id.toString()).removeValue()
            }
        }
    }
    
    fun clearLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            userId?.let {
                dao.clearUserHabits(it)
            }
        }
    }

    fun refreshStreaks() {
        val currentUserId = userId ?: return
        viewModelScope.launch {
            val habits = dao.getAllHabitsOnce(currentUserId)
            val now = System.currentTimeMillis()
            
            habits.forEach { habit ->
                if (!habit.isBroken && !habit.isArchived && habit.lastCompleted != 0L && !habit.isOneTime) {
                    if (isMissed(habit.lastCompleted, now)) {
                        updateHabit(habit.copy(isBroken = true))
                    }
                }
            }
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        if (time1 == 0L || time2 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isMissed(lastCompleted: Long, now: Long): Boolean {
        val calLast = Calendar.getInstance().apply { timeInMillis = lastCompleted }
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = calLast.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                          calLast.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
        calNow.add(Calendar.DAY_OF_YEAR, 1)
        val isToday = calLast.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                      calLast.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
        return !isToday && !isYesterday
    }
}
