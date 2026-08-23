package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.BondRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BondEngine @Inject constructor(
    private val bondRepository: BondRepository,
    private val activeCompanionManager: ActiveCompanionManager,
    private val activityEventRepository: ActivityEventRepository,
    private val companionNameResolver: CompanionNameResolver
) {
    /** Bond of the currently active companion (empty sentinel when none selected). */
    val bond: Flow<Bond> = activeCompanionManager.activeCompanionId.flatMapLatest { id ->
        if (id == null) flowOf(Bond(companionId = -1))
        else bondRepository.getBond(id)
    }

    suspend fun getBondDirect(companionId: Long): Bond = bondRepository.getBondDirect(companionId)

    suspend fun recordTap(companionId: Long) {
        mutateLevel(companionId, TAP_LEVEL_GAIN, countsAsTap = true)
    }

    suspend fun recordFeed(companionId: Long) {
        mutateLevel(companionId, FEED_LEVEL_GAIN, countsAsFeed = true)
    }

    suspend fun recordReminderCompleted(companionId: Long) {
        mutateLevel(companionId, REMINDER_COMPLETION_LEVEL_GAIN)
    }

    suspend fun recordTaskCompleted(companionId: Long) {
        mutateLevel(companionId, TASK_COMPLETION_LEVEL_GAIN)
    }

    suspend fun applyDecay() {
        bondRepository.getAllDirect().forEach { bond ->
            val daysInactive = (System.currentTimeMillis() - bond.lastInteractionTime) / DAY_IN_MILLIS
            val newLevel = if (daysInactive >= 1) (bond.level - 1).coerceAtLeast(0) else bond.level
            bondRepository.updateBond(
                bond.copy(
                    level = newLevel,
                    tapsToday = 0,
                    feedsToday = 0
                )
            )
        }
    }

    /**
     * Single mutation path for level gains so milestone logging can't diverge
     * between interaction kinds.
     */
    private suspend fun mutateLevel(
        companionId: Long,
        gain: Int,
        countsAsTap: Boolean = false,
        countsAsFeed: Boolean = false
    ) {
        val current = bondRepository.getBondDirect(companionId)
        if (countsAsTap && current.tapsToday >= MAX_TAPS_PER_DAY) return
        if (countsAsFeed && current.feedsToday >= MAX_FEEDS_PER_DAY) return

        val oldLevel = current.level
        val newLevel = (current.level + gain).coerceAtMost(MAX_LEVEL)
        bondRepository.updateBond(
            current.copy(
                level = newLevel,
                tapsToday = if (countsAsTap) current.tapsToday + 1 else current.tapsToday,
                feedsToday = if (countsAsFeed) current.feedsToday + 1 else current.feedsToday,
                totalInteractions = current.totalInteractions + 1,
                lastInteractionTime = System.currentTimeMillis()
            )
        )

        if (newLevel > oldLevel && isLevelMilestone(newLevel)) {
            activityEventRepository.record(
                companionId = companionId,
                type = ActivityType.BOND_LEVEL_UP,
                title = "${companionNameResolver.nameOf(companionId)} reached Bond Level $newLevel"
            )
        }
    }

    companion object {
        const val MAX_LEVEL = 100
        private const val MAX_TAPS_PER_DAY = 10
        private const val MAX_FEEDS_PER_DAY = 5
        private const val TAP_LEVEL_GAIN = 1
        private const val FEED_LEVEL_GAIN = 2
        private const val REMINDER_COMPLETION_LEVEL_GAIN = 3
        private const val TASK_COMPLETION_LEVEL_GAIN = 2
        private const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24

        /** Log "reached Bond Level X" only on 5-level milestones to avoid feed spam. */
        fun isLevelMilestone(level: Int): Boolean = level > 0 && level % 5 == 0
    }
}