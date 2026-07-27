package com.pixelpal.app.domain.usecase.reminder

import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.worker.ReminderScheduler
import javax.inject.Inject

class CreateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(reminder: Reminder): Long {
        val id = reminderRepository.insert(reminder)
        val created = reminder.copy(id = id)
        reminderScheduler.scheduleReminder(created)
        return id
    }
}
