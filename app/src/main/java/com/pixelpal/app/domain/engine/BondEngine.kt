package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.repository.BondRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BondEngine @Inject constructor(
    private val bondRepository: BondRepository
) {
    val bond: Flow<Bond> = bondRepository.getBond()

    suspend fun recordTap() {
        val current = bondRepository.getBondDirect()
        if (current.tapsToday < 5) {
            bondRepository.recordTap()
            val newLevel = (current.level + 1).coerceAtMost(100)
            bondRepository.updateBond(current.copy(level = newLevel))
        }
    }

    suspend fun recordFeed() {
        val current = bondRepository.getBondDirect()
        if (current.feedsToday < 3) {
            bondRepository.recordFeed()
            val newLevel = (current.level + 2).coerceAtMost(100)
            bondRepository.updateBond(current.copy(level = newLevel))
        }
    }

    suspend fun recordReminderCompleted() {
        val current = bondRepository.getBondDirect()
        val newLevel = (current.level + 3).coerceAtMost(100)
        bondRepository.updateBond(current.copy(level = newLevel, totalInteractions = current.totalInteractions + 1))
    }

    suspend fun applyDecay() {
        val current = bondRepository.getBondDirect()
        val daysInactive = (System.currentTimeMillis() - current.lastInteractionTime) / (1000 * 60 * 60 * 24)
        if (daysInactive >= 1) {
            val newLevel = (current.level - 1).coerceAtLeast(0)
            bondRepository.updateBond(current.copy(level = newLevel))
        }
        bondRepository.resetDailyCounts()
    }
}
