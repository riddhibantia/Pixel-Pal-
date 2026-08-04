package com.pixelpal.app.presentation.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.usecase.reminder.CompleteReminderUseCase
import com.pixelpal.app.domain.usecase.reminder.CreateReminderUseCase
import com.pixelpal.app.domain.usecase.reminder.GetRemindersUseCase
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    getRemindersUseCase: GetRemindersUseCase,
    private val createReminderUseCase: CreateReminderUseCase,
    private val completeReminderUseCase: CompleteReminderUseCase,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _reminderCreated = Channel<Boolean>()
    val reminderCreated = _reminderCreated.receiveAsFlow()

    val pendingReminders: StateFlow<List<Reminder>> = getRemindersUseCase.getPendingReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val completedReminders: StateFlow<List<Reminder>> = getRemindersUseCase.getCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createReminder(title: String, message: String?, triggerTime: Long, hour: Int, minute: Int, soundUri: String?) {
        viewModelScope.launch {
            val reminder = Reminder(
                title = title,
                message = message,
                triggerTime = triggerTime,
                hour = hour,
                minute = minute,
                soundUri = soundUri,
                category = "CUSTOM"
            )
            createReminderUseCase(reminder)
            _reminderCreated.send(true)
        }
    }

    fun completeReminder(id: Long) {
        viewModelScope.launch {
            completeReminderUseCase(id)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(reminder.id)
            reminderRepository.delete(reminder)
        }
    }
}
