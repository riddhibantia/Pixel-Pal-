package com.pixelpal.app.domain.usecase.reminder

import com.pixelpal.app.domain.engine.BondEngine
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.worker.ReminderScheduler
import javax.inject.Inject

class CompleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val bondEngine: BondEngine
) {
    suspend operator fun invoke(id: Long) {
        reminderScheduler.cancelReminder(id)
        reminderRepository.complete(id)
        bondEngine.recordReminderCompleted()
    }
}
