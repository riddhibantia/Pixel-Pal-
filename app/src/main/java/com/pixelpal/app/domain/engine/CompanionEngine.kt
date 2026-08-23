package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.Emotion
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.domain.usecase.reminder.SnoozeReminderUseCase
import com.pixelpal.app.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionEngine @Inject constructor(
    private val emotionEngine: EmotionEngine,
    private val bondEngine: BondEngine,
    private val reactionProvider: CompanionReactionProvider,
    private val overlayManager: OverlayManager,
    private val activeCompanionManager: ActiveCompanionManager,
    private val activityEventRepository: ActivityEventRepository,
    private val reminderRepository: ReminderRepository,
    private val snoozeReminderUseCase: SnoozeReminderUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Ordinary taps/feeds intentionally do NOT create ActivityEvents — they
     * only mutate bond/emotion and trigger a reaction. Meaningful events are
     * recorded elsewhere (bond milestones, tasks, reminders, agent changes).
     */
    fun onTap(companionId: Long) {
        scope.launch {
            bondEngine.recordTap(companionId)
            emotionEngine.triggerEmotion(Emotion.HAPPY)
            speakInteraction(companionId, CompanionReactionProvider.Interaction.TAP)
        }
    }

    fun onDoubleTap(companionId: Long) {
        scope.launch {
            bondEngine.recordTap(companionId)
            emotionEngine.triggerEmotion(Emotion.EXCITED)
            speakInteraction(companionId, CompanionReactionProvider.Interaction.DOUBLE_TAP)
        }
    }

    fun onFeed(companionId: Long) {
        scope.launch {
            bondEngine.recordFeed(companionId)
            emotionEngine.triggerEmotion(Emotion.HAPPY)
            speakInteraction(companionId, CompanionReactionProvider.Interaction.FEED)
        }
    }

    /** Convenience for callers that genuinely mean "whoever is active". */
    suspend fun resolveActiveCompanionId(): Long? =
        activeCompanionManager.getActiveCompanionIdDirect()

    private suspend fun speakInteraction(
        companionId: Long,
        interaction: CompanionReactionProvider.Interaction
    ) {
        if (!overlayManager.isShowing(companionId)) return

        val companion = activeCompanionManager.companionById(companionId) ?: return
        val bond = bondEngine.getBondDirect(companionId)
        val text = reactionProvider.interactionMessage(companion, bond.level, interaction)

        if (text != null) {
            overlayManager.showSpeechBubble(companionId, text)
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
                        val companionId = reminder.companionId
                            ?: activeCompanionManager.getActiveCompanionIdDirect()
                        if (companionId != null) {
                            bondEngine.recordReminderCompleted(companionId)
                            activityEventRepository.record(
                                companionId,
                                ActivityType.REMINDER_COMPLETED,
                                "Completed reminder \"${reminder.title}\""
                            )
                        }
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