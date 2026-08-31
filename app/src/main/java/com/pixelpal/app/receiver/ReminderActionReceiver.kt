package com.pixelpal.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.engine.BondEngine
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.util.Constants
import com.pixelpal.app.util.ReminderNotificationHelper
import com.pixelpal.app.worker.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var bondEngine: BondEngine
    @Inject lateinit var activityEventRepository: ActivityEventRepository
    @Inject lateinit var activeCompanionManager: ActiveCompanionManager
    @Inject lateinit var reminderNotificationHelper: ReminderNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Constants.ACTION_COMPLETE_REMINDER -> {
                        reminderRepository.complete(reminderId)
                        val reminder = reminderRepository.getById(reminderId)
                        val companionId = reminder?.companionId ?: activeCompanionManager.getActiveCompanionIdDirect()
                        if (companionId != null) {
                            bondEngine.recordReminderCompleted(companionId)
                            activityEventRepository.record(
                                companionId,
                                ActivityType.REMINDER_COMPLETED,
                                "Completed reminder \"${reminder?.title ?: "Reminder"}\""
                            )
                        }
                        reminderNotificationHelper.cancel(reminderId)
                    }
                    Constants.ACTION_SNOOZE_REMINDER -> {
                        val newTriggerTime = System.currentTimeMillis() + 10 * 60 * 1000
                        reminderRepository.snooze(reminderId, newTriggerTime)
                        val updatedReminder = reminderRepository.getById(reminderId)
                        if (updatedReminder != null) {
                            reminderScheduler.scheduleReminder(updatedReminder)
                        }
                        reminderNotificationHelper.cancel(reminderId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
