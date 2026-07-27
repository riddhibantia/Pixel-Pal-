package com.pixelpal.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.triggerTime,
            pendingIntent
        )
    }

    fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    suspend fun rescheduleAll() {
        val pendingReminders = reminderRepository.getPendingReminders().first()
        for (reminder in pendingReminders) {
            if (reminder.triggerTime > System.currentTimeMillis()) {
                scheduleReminder(reminder)
            }
        }
    }
}
