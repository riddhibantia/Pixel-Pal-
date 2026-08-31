package com.pixelpal.app.presentation.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.usecase.companion.GetActiveCompanionUseCase
import com.pixelpal.app.domain.usecase.reminder.CompleteReminderUseCase
import com.pixelpal.app.domain.usecase.reminder.CreateReminderUseCase
import com.pixelpal.app.domain.usecase.reminder.GetRemindersUseCase
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.presentation.components.SnackbarEvent
import com.pixelpal.app.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reminders belong to THE single companion — created against the primary id.
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val getActiveCompanionUseCase: GetActiveCompanionUseCase,
    getRemindersUseCase: GetRemindersUseCase,
    private val createReminderUseCase: CreateReminderUseCase,
    private val completeReminderUseCase: CompleteReminderUseCase,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _reminderCreated = Channel<Boolean>()
    val reminderCreated = _reminderCreated.receiveAsFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    val pendingReminders: StateFlow<List<Reminder>> = getRemindersUseCase.getPendingReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val completedReminders: StateFlow<List<Reminder>> = getRemindersUseCase.getCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createReminder(
        title: String,
        message: String?,
        triggerTime: Long,
        hour: Int,
        minute: Int,
        soundUri: String?
    ) {
        viewModelScope.launch {
            val companionId = getActiveCompanionUseCase.activeCompanion.first()?.id
            val reminder = Reminder(
                title = title,
                message = message,
                triggerTime = triggerTime,
                hour = hour,
                minute = minute,
                soundUri = soundUri,
                category = "CUSTOM",
                companionId = companionId
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
            _snackbarEvents.emit(SnackbarEvent.ReminderDeleted(reminder))
        }
    }

    fun undoDeleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.insert(reminder)
            if (reminder.triggerTime > System.currentTimeMillis()) {
                reminderScheduler.scheduleReminder(reminder)
            }
        }
    }
}