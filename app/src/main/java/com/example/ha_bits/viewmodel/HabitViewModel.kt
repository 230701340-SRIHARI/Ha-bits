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
import kotlinx.coroutines.launch
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
                        val habitMap = habitSnapshot.value as? Map<String, Any>
                        if (habitMap != null) {
                            val habit = Habit(
                                id = (habitMap["id"] as? Long)?.toInt() ?: 0,
                                userId = currentUserId,
                                name = habitMap["name"] as? String ?: "",
                                streak = (habitMap["streak"] as? Long)?.toInt() ?: 0,
                                lastCompleted = habitMap["lastCompleted"] as? Long ?: 0L,
                                isCustom = habitMap["isCustom"] as? Boolean ?: false,
                                isBroken = habitMap["isBroken"] as? Boolean ?: false,
                                reminderHour = (habitMap["reminderHour"] as? Long)?.toInt() ?: 12,
                                reminderMinute = (habitMap["reminderMinute"] as? Long)?.toInt() ?: 0,
                                isOneTime = habitMap["isOneTime"] as? Boolean ?: false,
                                isArchived = habitMap["isArchived"] as? Boolean ?: false
                            )
                            firebaseHabits.add(habit)
                        }
                    }
                    firebaseHabits.forEach { dao.insert(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun completeHabit(habit: Habit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (isSameDay(habit.lastCompleted, now)) return@launch

            val updatedHabit = if (habit.isBroken) {
                habit
            } else {
                val newStreak = habit.streak + 1
                if (habit.isOneTime) {
                    habit.copy(streak = newStreak, lastCompleted = now, isArchived = true)
                } else {
                    habit.copy(streak = newStreak, lastCompleted = now)
                }
            }
            
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
            dao.insert(newHabit)
            val latestHabits = dao.getAllHabitsOnce(currentUserId)
            db.child(currentUserId).child("habits").setValue(latestHabits)
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