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
        if (current.tapsToday < MAX_TAPS_PER_DAY) {
            bondRepository.updateBond(
                current.copy(
                    level = (current.level + TAP_LEVEL_GAIN).coerceAtMost(MAX_LEVEL),
                    tapsToday = current.tapsToday + 1,
                    totalInteractions = current.totalInteractions + 1,
                    lastInteractionTime = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun recordFeed() {
        val current = bondRepository.getBondDirect()
        if (current.feedsToday < MAX_FEEDS_PER_DAY) {
            bondRepository.updateBond(
                current.copy(
                    level = (current.level + FEED_LEVEL_GAIN).coerceAtMost(MAX_LEVEL),
                    feedsToday = current.feedsToday + 1,
                    totalInteractions = current.totalInteractions + 1,
                    lastInteractionTime = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun recordReminderCompleted() {
        val current = bondRepository.getBondDirect()
        val newLevel = (current.level + 3).coerceAtMost(100)
        bondRepository.updateBond(current.copy(level = newLevel, totalInteractions = current.totalInteractions + 1))
    }

    suspend fun applyDecay() {
        val current = bondRepository.getBondDirect()
        val daysInactive = (System.currentTimeMillis() - current.lastInteractionTime) / DAY_IN_MILLIS
        if (daysInactive >= 1) {
            val newLevel = (current.level - 1).coerceAtLeast(0)
            bondRepository.updateBond(current.copy(level = newLevel))
        }
        bondRepository.resetDailyCounts()
    }

    companion object {
        const val MAX_LEVEL = 100
        private const val MAX_TAPS_PER_DAY = 5
        private const val MAX_FEEDS_PER_DAY = 3
        private const val TAP_LEVEL_GAIN = 1
        private const val FEED_LEVEL_GAIN = 2
        private const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24
    }
}
