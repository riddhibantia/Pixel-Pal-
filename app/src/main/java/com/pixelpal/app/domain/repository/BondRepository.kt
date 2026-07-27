package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Bond
import kotlinx.coroutines.flow.Flow

interface BondRepository {
    fun getBond(): Flow<Bond>
    suspend fun getBondDirect(): Bond
    suspend fun updateBond(bond: Bond)
    suspend fun recordTap()
    suspend fun recordFeed()
    suspend fun resetDailyCounts()
}
