package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.BondRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bond & streak progression for THE companion.
 *
 * Meaningful actions (tasks/reminders) are the main source of bond; casual
 * taps only grant level for the first [BOND_GRANTING_TAPS_PER_DAY] per day —
 * beyond that they trigger reactions without inflating progress.
 * Streaks increment once per calendar day of interaction, with milestone events.
 */
@Singleton
class BondEngine @Inject constructor(
    private val bondRepository: BondRepository,
    private val activeCompanionManager: ActiveCompanionManager,
    private val activityEventRepository: ActivityEventRepository,
    private val companionNameResolver: CompanionNameResolver
) {
    /** Bond of the primary companion (empty sentinel when none exists). */
    val bond: Flow<Bond> = activeCompanionManager.activeCompanionId.flatMapLatest { id ->
        if (id == null) flowOf(Bond(companionId = -1))
        else bondRepository.getBond(id)
    }

    suspend fun getBondDirect(companionId: Long): Bond = bondRepository.getBondDirect(companionId)

    suspend fun recordTap(companionId: Long) {
        mutateLevel(companionId, TAP_LEVEL_GAIN, countsAsTap = true)
    }

    suspend fun recordFeed(companionId: Long) {
        // Cosmetic interaction: NO bond gain by design (anti-spam).
        touchInteraction(companionId)
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
     * Single mutation path for level gains so streak/milestone logging can't
     * diverge between interaction kinds.
     */
    private suspend fun mutateLevel(
        companionId: Long,
        gain: Int,
        countsAsTap: Boolean = false,
        countsAsFeed: Boolean = false
    ) {
        val current = bondRepository.getBondDirect(companionId)
        if (countsAsTap && current.tapsToday >= MAX_TAPS_PER_DAY) return

        // Anti-spam: taps stop granting LEVEL after the daily meaningful budget.
        val effectiveGain = if (countsAsTap && current.tapsToday >= BOND_GRANTING_TAPS_PER_DAY) 0 else gain

        val oldLevel = current.level
        val today = todayKey()
        val isNewDay = current.lastStreakDate != today
        val newStreak = if (isNewDay) computeStreak(current.streakDays, current.lastStreakDate) else current.streakDays

        val newLevel = (current.level + effectiveGain).coerceAtMost(MAX_LEVEL)
        bondRepository.updateBond(
            current.copy(
                level = newLevel,
                tapsToday = if (countsAsTap) current.tapsToday + 1 else current.tapsToday,
                totalInteractions = current.totalInteractions + 1,
                lastInteractionTime = System.currentTimeMillis(),
                streakDays = newStreak,
                lastStreakDate = if (isNewDay) today else current.lastStreakDate
            )
        )

        if (isNewDay && isStreakMilestone(newStreak)) {
            activityEventRepository.record(
                companionId = companionId,
                type = ActivityType.STREAK_MILESTONE,
                title = "${companionNameResolver.nameOf(companionId)} reached a $newStreak-day streak"
            )
        }

        if (newLevel > oldLevel && isLevelMilestone(newLevel)) {
            activityEventRepository.record(
                companionId = companionId,
                type = ActivityType.BOND_LEVEL_UP,
                title = "${companionNameResolver.nameOf(companionId)} reached Bond Level $newLevel"
            )
        }
    }

    /** Cosmetic-only path (feed/play): updates recency + streak, never level. */
    private suspend fun touchInteraction(companionId: Long) {
        val current = bondRepository.getBondDirect(companionId)
        val today = todayKey()
        val isNewDay = current.lastStreakDate != today
        val newStreak = if (isNewDay) computeStreak(current.streakDays, current.lastStreakDate) else current.streakDays

        bondRepository.updateBond(
            current.copy(
                totalInteractions = current.totalInteractions + 1,
                lastInteractionTime = System.currentTimeMillis(),
                streakDays = newStreak,
                lastStreakDate = if (isNewDay) today else current.lastStreakDate
            )
        )
    }

    /**
     * Streak continues (+1) when the previous day interacted; resets to 1
     * after a gap of 2+ days.
     */
    private fun computeStreak(currentStreak: Int, lastStreakDate: String): Int {
        if (lastStreakDate.isBlank()) return 1
        return try {
            val fmt = SimpleDateFormat(DATE_FORMAT, Locale.US)
            val last = fmt.parse(lastStreakDate) ?: return 1
            val daysBetween = (System.currentTimeMillis() - last.time) / DAY_IN_MILLIS
            when {
                daysBetween <= 1L -> currentStreak + 1
                else -> 1
            }
        } catch (_: Exception) {
            1
        }
    }

    companion object {
        const val MAX_LEVEL = 100
        private const val MAX_TAPS_PER_DAY = 10
        const val BOND_GRANTING_TAPS_PER_DAY = 3
        private const val TAP_LEVEL_GAIN = 1
        private const val REMINDER_COMPLETION_LEVEL_GAIN = 3
        private const val TASK_COMPLETION_LEVEL_GAIN = 2
        private const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24
        private const val DATE_FORMAT = "yyyyMMdd"

        private val STREAK_MILESTONES = setOf(3, 7, 14, 30, 60, 100)

        /** Log "reached Bond Level X" only on 5-level milestones to avoid feed spam. */
        fun isLevelMilestone(level: Int): Boolean = level > 0 && level % 5 == 0

        fun isStreakMilestone(streak: Int): Boolean = streak in STREAK_MILESTONES

        fun todayKey(): String =
            SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
    }
}