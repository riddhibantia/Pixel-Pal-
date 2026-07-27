package com.pixelpal.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelpal.app.domain.engine.CompanionEngine
import com.pixelpal.app.domain.repository.ReminderRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var companionEngine: CompanionEngine
    @Inject lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        Timber.d("AlarmReceiver triggered for reminderId: $reminderId")

        if (reminderId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminder = reminderRepository.getById(reminderId)
                    if (reminder != null) {
                        companionEngine.onReminderTriggered(reminder)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
