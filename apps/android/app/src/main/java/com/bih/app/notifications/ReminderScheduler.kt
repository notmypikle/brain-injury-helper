package com.bih.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bih.app.data.Reminder
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// Schedules a local notification for each upcoming reminder.
// All work happens on the device — reminders never leave the phone.
object ReminderScheduler {

    const val CHANNEL_ID = "reminders"
    private const val WORK_PREFIX = "reminder-"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Due reminders from B.I.H." }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    // Re-schedule everything from the current reminder list (called whenever
    // the Today screen loads). Done/past reminders get no notification.
    fun scheduleAll(context: Context, reminders: List<Reminder>) {
        val wm = WorkManager.getInstance(context)
        reminders.filter { !it.done }.forEach { reminder ->
            val dueMs = dueAtMillis(reminder.due_at) ?: return@forEach
            val delayMs = dueMs - System.currentTimeMillis()
            if (delayMs <= 0) return@forEach
            val work = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putInt(ReminderWorker.KEY_ID, reminder.id)
                        .putString(ReminderWorker.KEY_TEXT, reminder.text)
                        .build(),
                )
                .build()
            wm.enqueueUniqueWork(
                WORK_PREFIX + reminder.id,
                ExistingWorkPolicy.REPLACE,
                work,
            )
        }
    }

    fun cancel(context: Context, reminderId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + reminderId)
    }

    // due_at is ISO 8601; with offset if present, otherwise local time (risk P2).
    fun dueAtMillis(dueAt: String): Long? = try {
        OffsetDateTime.parse(dueAt).toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(dueAt).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e2: Exception) {
            null
        }
    }
}
