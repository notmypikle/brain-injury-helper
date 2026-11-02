package com.bih.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// Shows one reminder notification when its due time arrives.
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val text = inputData.getString(KEY_TEXT) ?: return Result.success()
        val id = inputData.getInt(KEY_ID, 0)

        // POST_NOTIFICATIONS may have been revoked since scheduling.
        val granted = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Reminder")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
        return Result.success()
    }

    companion object {
        const val KEY_ID = "reminder_id"
        const val KEY_TEXT = "reminder_text"
    }
}
