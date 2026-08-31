package com.pixelpal.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pixelpal.app.R
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.presentation.MainActivity
import com.pixelpal.app.receiver.ReminderActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun show(reminder: Reminder) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return

        ensureChannel(manager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = Constants.ACTION_COMPLETE_REMINDER
            putExtra("reminder_id", reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = Constants.ACTION_SNOOZE_REMINDER
            putExtra("reminder_id", reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_companion)
            .setContentTitle(reminder.title)
            .setContentText(reminder.message ?: "It's time!")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Snooze 10 min", snoozePendingIntent)
            .build()

        manager.notify(Constants.NOTIFICATION_ID_REMINDER + reminder.id.toInt(), notification)
    }

    fun cancel(reminderId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(Constants.NOTIFICATION_ID_REMINDER + reminderId.toInt())
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_REMINDER,
                Constants.CHANNEL_REMINDER_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
