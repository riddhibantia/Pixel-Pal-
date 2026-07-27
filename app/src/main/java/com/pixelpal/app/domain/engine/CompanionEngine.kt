package com.pixelpal.app.domain.engine

import com.pixelpal.app.data.dialogue.DialogueLoader
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.Emotion
import com.pixelpal.app.domain.model.Reminder
import com.pixelpal.app.domain.repository.ReminderRepository
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
    private val reminderRepository: ReminderRepository
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

            val petName = preferencesManager.petName.first()
            val userName = preferencesManager.userName.first().ifEmpty { "friend" }
            val bond = bondEngine.bond.first()

            val text = dialogueLoader.getLine(
                contextStr = "reminder_trigger",
                emotion = Emotion.THINKING,
                bondLevel = bond.level,
                variables = mapOf(
                    "pet_name" to petName,
                    "user_name" to userName,
                    "title" to reminder.title
                )
            ) ?: "Hey $userName! Time for ${reminder.title}!"

            if (overlayManager.isShowing()) {
                overlayManager.showSpeechBubble(
                    text = text,
                    onDone = {
                        scope.launch {
                            reminderRepository.complete(reminder.id)
                            bondEngine.recordReminderCompleted()
                            emotionEngine.triggerEmotion(Emotion.HAPPY)
                        }
                    },
                    onSnooze = {
                        scope.launch {
                            val newTime = System.currentTimeMillis() + (15 * 60 * 1000L)
                            reminderRepository.snooze(reminder.id, newTime)
                            emotionEngine.triggerEmotion(Emotion.CALM)
                        }
                    },
                    onDismiss = {
                        scope.launch {
                            emotionEngine.triggerEmotion(Emotion.SAD, durationMs = 10_000L)
                        }
                    }
                )
            }
        }
    }
}
