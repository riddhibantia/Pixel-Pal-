package com.pixelpal.app.domain.usecase.reminder

import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    fun getPendingReminders(): Flow<List<Reminder>> {
        return reminderRepository.getPendingReminders()
    }

    fun getCompletedReminders(): Flow<List<Reminder>> {
        return reminderRepository.getCompletedReminders()
    }
}
