package com.example.ha_bits.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.ha_bits.R

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val builder = NotificationCompat.Builder(applicationContext, "HABIT_CH")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Habit Check-in!")
            .setContentText("Keep your streaks alive. Complete your tasks now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(applicationContext).notify(1, builder.build())
        return Result.success()
    }
}