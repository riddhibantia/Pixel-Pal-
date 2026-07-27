package com.pixelpal.app.presentation.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.usecase.reminder.CompleteReminderUseCase
import com.pixelpal.app.domain.usecase.reminder.CreateReminderUseCase
import com.pixelpal.app.domain.usecase.reminder.GetRemindersUseCase
import com.pixelpal.app.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    getRemindersUseCase: GetRemindersUseCase,
    private val createReminderUseCase: CreateReminderUseCase,
    private val completeReminderUseCase: CompleteReminderUseCase,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val pendingReminders: StateFlow<List<Reminder>> = getRemindersUseCase.getPendingReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val completedReminders: StateFlow<List<Reminder>> = getRemindersUseCase.getCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createReminder(title: String, message: String?, triggerTime: Long) {
        viewModelScope.launch {
            val reminder = Reminder(
                title = title,
                message = message,
                triggerTime = triggerTime,
                category = "CUSTOM"
            )
            createReminderUseCase(reminder)
        }
    }

    fun completeReminder(id: Long) {
        viewModelScope.launch {
            completeReminderUseCase(id)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.delete(reminder)
        }
    }
}
