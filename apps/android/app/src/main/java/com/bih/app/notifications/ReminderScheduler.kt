package com.bih.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

    // Exact alarms need a user grant on Android 12+ (SCHEDULE_EXACT_ALARM).
    // Below 31 it's automatic with the permission declared.
    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true)

    // Re-schedule everything from the current reminder list. Done/past
    // reminders get no notification. When exact alarms are allowed we use
    // AlarmManager (fires on time even in Doze — user worry 2026-11-11:
    // "not sure an alarm will actually go off"); otherwise WorkManager is
    // the fallback and the Today screen nudges the user to allow exact.
    fun scheduleAll(context: Context, reminders: List<Reminder>) {
        val wm = WorkManager.getInstance(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val exact = exactAlarmsAllowed(context)
        reminders.filter { !it.done }.forEach { reminder ->
            val dueMs = dueAtMillis(reminder.due_at) ?: return@forEach
            if (dueMs <= System.currentTimeMillis()) return@forEach
            if (exact && am != null) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, dueMs, alarmIntent(context, reminder),
                )
                wm.cancelUniqueWork(WORK_PREFIX + reminder.id)
            } else {
                val work = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(dueMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
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
    }

    fun cancel(context: Context, reminderId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + reminderId)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, reminderId,
            Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am?.cancel(pi)
    }

    private fun alarmIntent(context: Context, reminder: Reminder): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            reminder.id,
            Intent(context, ReminderAlarmReceiver::class.java)
                .putExtra(ReminderWorker.KEY_ID, reminder.id)
                .putExtra(ReminderWorker.KEY_TEXT, reminder.text),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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
