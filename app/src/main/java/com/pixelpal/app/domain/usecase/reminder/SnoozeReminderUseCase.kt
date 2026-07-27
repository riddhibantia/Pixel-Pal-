package com.pixelpal.app.domain.usecase.reminder

import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.worker.ReminderScheduler
import javax.inject.Inject

class SnoozeReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(id: Long, minutes: Int = 15) {
        val newTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        reminderRepository.snooze(id, newTime)
        val reminder = reminderRepository.getById(id)
        reminder?.let { reminderScheduler.scheduleReminder(it) }
    }
}
