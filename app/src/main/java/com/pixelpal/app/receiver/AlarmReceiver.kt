package com.pixelpal.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import com.pixelpal.app.domain.engine.CompanionEngine
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.worker.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.util.ReminderNotificationHelper

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var companionEngine: CompanionEngine
    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var reminderNotificationHelper: ReminderNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        Timber.d("AlarmReceiver triggered for reminderId: $reminderId")

        if (reminderId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminder = reminderRepository.getById(reminderId)
                    if (reminder != null) {
                        playRingtone(context, reminder)
                        vibrate(context)

                        // Always post a system notification — visible on lock screen and notification shade
                        reminderNotificationHelper.show(reminder)

                        companionEngine.onReminderTriggered(reminder)

                        advanceSchedule(reminder)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    /**
     * Re-arms recurring reminders for their next occurrence; one-shot reminders are
     * marked TRIGGERED so they stop counting as pending.
     */
    private suspend fun advanceSchedule(reminder: Reminder) {
        try {
            val next = reminderScheduler.nextTriggerTime(reminder)
            if (next != null) {
                val updated = reminder.copy(triggerTime = next, snoozeCount = 0)
                reminderRepository.update(updated)
                reminderScheduler.scheduleReminder(updated)
            } else {
                reminderRepository.update(reminder.copy(status = "TRIGGERED"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to advance schedule for reminder ${reminder.id}")
        }
    }

    private fun playRingtone(context: Context, reminder: Reminder) {
        try {
            val uri = reminder.soundUri?.let { android.net.Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()

            // Stop ringtone after 5 seconds to avoid annoying the user if they don't act
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (ringtone?.isPlaying == true) ringtone.stop()
            }, 5000)
        } catch (e: Exception) {
            Timber.e(e, "Error playing reminder ringtone")
        }
    }

    private fun vibrate(context: Context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
                vibratorManager?.defaultVibrator?.vibrate(
                    android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.vibrate(
                    android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error vibrating for reminder")
        }
    }
}
