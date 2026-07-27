package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Companion
import kotlinx.coroutines.flow.Flow

interface CompanionRepository {
    fun getCompanion(): Flow<Companion>
    suspend fun getCompanionDirect(): Companion
    suspend fun updateCompanion(companion: Companion)
}
