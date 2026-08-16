package com.pixelpal.app.domain.engine

import com.pixelpal.app.data.dialogue.DialogueLoader
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.Emotion
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.usecase.reminder.SnoozeReminderUseCase
import com.pixelpal.app.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionEngine @Inject constructor(
    private val emotionEngine: EmotionEngine,
    private val bondEngine: BondEngine,
    private val personalityEngine: PersonalityEngine,
    private val dialogueLoader: DialogueLoader,
    private val overlayManager: OverlayManager,
    private val preferencesManager: PreferencesManager,
    private val reminderRepository: ReminderRepository,
    private val snoozeReminderUseCase: SnoozeReminderUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun onTap() {
        scope.launch {
            bondEngine.recordTap()
            emotionEngine.triggerEmotion(Emotion.HAPPY)

            val petName = preferencesManager.petName.first()
            val userName = preferencesManager.userName.first().ifEmpty { "friend" }
            val bond = bondEngine.bond.first()

            val text = dialogueLoader.getLine(
                contextStr = "tap_response",
                emotion = Emotion.HAPPY,
                bondLevel = bond.level,
                variables = mapOf("pet_name" to petName, "user_name" to userName)
            )

            if (text != null && overlayManager.isShowing()) {
                overlayManager.showSpeechBubble(text)
            }
        }
    }

    fun onDoubleTap() {
        scope.launch {
            bondEngine.recordTap()
            emotionEngine.triggerEmotion(Emotion.EXCITED)

            val petName = preferencesManager.petName.first()
            val userName = preferencesManager.userName.first().ifEmpty { "friend" }
            val bond = bondEngine.bond.first()

            val text = dialogueLoader.getLine(
                contextStr = "double_tap_response",
                emotion = Emotion.EXCITED,
                bondLevel = bond.level,
                variables = mapOf("pet_name" to petName, "user_name" to userName)
            )

            if (text != null && overlayManager.isShowing()) {
                overlayManager.showSpeechBubble(text)
            }
        }
    }

    fun onFeed() {
        scope.launch {
            bondEngine.recordFeed()
            emotionEngine.triggerEmotion(Emotion.HAPPY)

            val petName = preferencesManager.petName.first()
            val userName = preferencesManager.userName.first().ifEmpty { "friend" }
            val bond = bondEngine.bond.first()

            val text = dialogueLoader.getLine(
                contextStr = "feed_response",
                emotion = Emotion.HAPPY,
                bondLevel = bond.level,
                variables = mapOf("pet_name" to petName, "user_name" to userName)
            )

            if (text != null && overlayManager.isShowing()) {
                overlayManager.showSpeechBubble(text)
            }
        }
    }

    fun onReminderTriggered(reminder: Reminder) {
        scope.launch {
            emotionEngine.triggerEmotion(Emotion.THINKING, durationMs = 60_000L)

            if (!overlayManager.isShowing()) return@launch

            // Recurring reminders were already re-armed for their next occurrence when
            // the alarm fired; completing one records the win without killing the schedule.
            val isRecurring =
                reminder.recurrence != "ONCE" || (reminder.recurrenceInterval ?: 0L) > 0L

            overlayManager.showDynamicIsland(
                title = reminder.title,
                timeLabel = formatTime(reminder.triggerTime),
                note = reminder.message,
                onComplete = {
                    scope.launch {
                        if (!isRecurring) {
                            reminderRepository.complete(reminder.id)
                        }
                        bondEngine.recordReminderCompleted()
                        emotionEngine.triggerEmotion(Emotion.HAPPY)
                    }
                },
                onSnooze = {
                    scope.launch {
                        snoozeReminderUseCase(reminder.id, minutes = 10)
                        emotionEngine.triggerEmotion(Emotion.THINKING, durationMs = 10_000L)
                    }
                }
            )
        }
    }

    private fun formatTime(triggerTime: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = triggerTime }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return String.format("%d:%02d %s", h12, minute, amPm)
    }
}
