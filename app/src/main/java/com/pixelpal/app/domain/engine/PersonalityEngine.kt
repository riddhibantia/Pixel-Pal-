package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.Personality
import com.pixelpal.app.domain.repository.PersonalityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class DailyInteractionStats(
    val tapCount: Int,
    val feedCount: Int,
    val remindersCompleted: Int,
    val remindersIgnored: Int,
    val isLateNightActive: Boolean,
    val isMorningActive: Boolean
)

@Singleton
class PersonalityEngine @Inject constructor(
    private val personalityRepository: PersonalityRepository
) {
    fun getPersonality(companionId: Long): Flow<Personality> =
        personalityRepository.getPersonality(companionId)

    suspend fun getPersonalityDirect(companionId: Long): Personality {
        return personalityRepository.getPersonalityDirect(companionId)
    }

    suspend fun updatePersonality(personality: Personality) {
        personalityRepository.updatePersonality(personality)
    }

    suspend fun recalculateDaily(companionId: Long, stats: DailyInteractionStats) {
        val current = getPersonalityDirect(companionId)

        var newFriendliness = current.friendliness
        var newCuriosity = current.curiosity
        var newPlayfulness = current.playfulness
        var newSleepiness = current.sleepiness
        var newConfidence = current.confidence
        var newIndependence = current.independence

        // Taps adjustment
        if (stats.tapCount > 10) {
            newFriendliness += 0.02f
            newPlayfulness += 0.01f
        } else if (stats.tapCount < 3) {
            newIndependence += 0.02f
            newSleepiness += 0.01f
        }

        // Reminders adjustment
        if (stats.remindersCompleted > stats.remindersIgnored) {
            newConfidence += 0.02f
        } else if (stats.remindersIgnored > 0) {
            newIndependence += 0.02f
            newConfidence -= 0.01f
        }

        // Time of day adjustment
        if (stats.isLateNightActive) {
            newSleepiness -= 0.01f
            newCuriosity += 0.01f
        }
        if (stats.isMorningActive) {
            newCuriosity += 0.01f
        }

        // Feeding adjustment
        if (stats.feedCount > 0) {
            newFriendliness += 0.01f
        }

        val updated = Personality(
            companionId = companionId,
            friendliness = newFriendliness.coerceIn(0.0f, 1.0f),
            curiosity = newCuriosity.coerceIn(0.0f, 1.0f),
            playfulness = newPlayfulness.coerceIn(0.0f, 1.0f),
            sleepiness = newSleepiness.coerceIn(0.0f, 1.0f),
            confidence = newConfidence.coerceIn(0.0f, 1.0f),
            independence = newIndependence.coerceIn(0.0f, 1.0f),
            lastUpdated = System.currentTimeMillis()
        )

        updatePersonality(updated)
    }
}