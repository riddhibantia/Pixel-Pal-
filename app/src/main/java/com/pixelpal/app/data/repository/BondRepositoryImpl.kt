package com.pixelpal.app.data.repository

import com.pixelpal.app.data.local.db.dao.BondDao
import com.pixelpal.app.data.local.db.entity.BondEntity
import com.pixelpal.app.domain.model.Bond
import com.pixelpal.app.domain.repository.BondRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BondRepositoryImpl @Inject constructor(
    private val dao: BondDao
) : BondRepository {

    override fun getBond(): Flow<Bond> {
        return dao.getBond().map { entity ->
            entity?.toDomain() ?: Bond()
        }
    }

    override suspend fun getBondDirect(): Bond {
        return dao.getBondDirect()?.toDomain() ?: Bond()
    }

    override suspend fun updateBond(bond: Bond) {
        dao.insertOrUpdate(bond.toEntity())
    }

    override suspend fun recordTap() {
        dao.recordTap()
    }

    override suspend fun recordFeed() {
        dao.recordFeed()
    }

    override suspend fun resetDailyCounts() {
        dao.resetDailyCounts()
    }

    private fun BondEntity.toDomain() = Bond(
        id = id,
        level = level,
        totalInteractions = totalInteractions,
        tapsToday = tapsToday,
        feedsToday = feedsToday,
        lastInteractionTime = lastInteractionTime,
        streakDays = streakDays,
        lastStreakDate = lastStreakDate
    )

    private fun Bond.toEntity() = BondEntity(
        id = id,
        level = level,
        totalInteractions = totalInteractions,
        tapsToday = tapsToday,
        feedsToday = feedsToday,
        lastInteractionTime = lastInteractionTime,
        streakDays = streakDays,
        lastStreakDate = lastStreakDate
    )
}
