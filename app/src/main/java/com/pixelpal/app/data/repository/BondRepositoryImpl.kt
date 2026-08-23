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

    override fun getBond(companionId: Long): Flow<Bond> {
        return dao.getBond(companionId).map { entity ->
            entity?.toDomain() ?: Bond(companionId = companionId)
        }
    }

    override suspend fun getBondDirect(companionId: Long): Bond {
        return dao.getBondDirect(companionId)?.toDomain() ?: Bond(companionId = companionId)
    }

    override suspend fun getAllDirect(): List<Bond> = dao.getAllDirect().map { it.toDomain() }

    override suspend fun updateBond(bond: Bond) {
        dao.insertOrUpdate(bond.toEntity())
    }

    override suspend fun ensureExists(companionId: Long) {
        if (dao.getBondDirect(companionId) == null) {
            dao.insertOrUpdate(BondEntity(companionId = companionId))
        }
    }

    private fun BondEntity.toDomain() = Bond(
        companionId = companionId,
        level = level,
        totalInteractions = totalInteractions,
        tapsToday = tapsToday,
        feedsToday = feedsToday,
        lastInteractionTime = lastInteractionTime,
        streakDays = streakDays,
        lastStreakDate = lastStreakDate
    )

    private fun Bond.toEntity() = BondEntity(
        companionId = companionId,
        level = level,
        totalInteractions = totalInteractions,
        tapsToday = tapsToday,
        feedsToday = feedsToday,
        lastInteractionTime = lastInteractionTime,
        streakDays = streakDays,
        lastStreakDate = lastStreakDate
    )
}