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
import timber.log.Timber
import javax.inject.Inject
import com.pixelpal.app.domain.model.Reminder

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var companionEngine: CompanionEngine
    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        Timber.d("AlarmReceiver triggered for reminderId: $reminderId")

        if (reminderId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminder = reminderRepository.getById(reminderId)
                    if (reminder != null) {
                        // The Dynamic Island overlay is the reminder UI — no system
                        // notification popup. The ringtone still plays.
                        playRingtone(context, reminder)

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
}
