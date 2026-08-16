package com.pixelpal.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Calendar
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

        val triggerTime = reminder.triggerTime

        // Exact alarms require SCHEDULE_EXACT_ALARM on Android 12+ (unless exempt).
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Fallback: inexact alarm still fires, just not to the second.
                Timber.w("Exact alarm permission missing for reminder ${reminder.id}, using inexact alarm")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException scheduling reminder ${reminder.id}, falling back to inexact alarm")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (e2: Exception) {
                Timber.e(e2, "Failed to schedule fallback alarm for reminder ${reminder.id}")
            }
        }
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
        val now = System.currentTimeMillis()
        for (reminder in pendingReminders) {
            if (reminder.triggerTime > now) {
                scheduleReminder(reminder)
            } else {
                // Past due: advance recurring reminders to their next occurrence so a
                // missed period doesn't pause the schedule forever.
                val next = nextTriggerTime(reminder, now)
                if (next != null) {
                    val updated = reminder.copy(triggerTime = next, snoozeCount = 0)
                    reminderRepository.update(updated)
                    scheduleReminder(updated)
                }
            }
        }
    }

    /**
     * Next trigger time for a recurring reminder after [from], or null for one-shot
     * (ONCE) reminders. Rolls the original trigger time forward by whole periods so
     * the schedule stays anchored to the time the user picked.
     */
    fun nextTriggerTime(reminder: Reminder, from: Long = System.currentTimeMillis()): Long? {
        val interval = reminder.recurrenceInterval
        return when {
            interval != null && interval > 0 -> {
                var next = reminder.triggerTime
                while (next <= from) next += interval
                next
            }
            reminder.recurrence == "DAILY" || reminder.recurrence == "WEEKLY" || reminder.recurrence == "MONTHLY" -> {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = reminder.triggerTime
                    if (reminder.hour > 0 || reminder.minute > 0) {
                        set(Calendar.HOUR_OF_DAY, reminder.hour)
                        set(Calendar.MINUTE, reminder.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
                val step = when (reminder.recurrence) {
                    "DAILY" -> Calendar.DAY_OF_YEAR
                    "WEEKLY" -> Calendar.WEEK_OF_YEAR
                    else -> Calendar.MONTH
                }
                while (calendar.timeInMillis <= from) calendar.add(step, 1)
                calendar.timeInMillis
            }
            else -> null
        }
    }
}
