package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Bond
import kotlinx.coroutines.flow.Flow

interface BondRepository {
    fun getBond(companionId: Long): Flow<Bond>
    suspend fun getBondDirect(companionId: Long): Bond
    suspend fun getAllDirect(): List<Bond>
    suspend fun updateBond(bond: Bond)
    /** Creates a default bond row for the companion if one does not exist. */
    suspend fun ensureExists(companionId: Long)
}