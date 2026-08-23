package com.pixelpal.app.domain.engine

import com.pixelpal.app.domain.repository.CompanionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves display names for companion-scoped messages (activity events,
 * reactions) without leaking the repository into engines.
 */
@Singleton
class CompanionNameResolver @Inject constructor(
    private val companionRepository: CompanionRepository
) {
    suspend fun nameOf(companionId: Long): String =
        companionRepository.getByIdDirect(companionId)?.name ?: "Your companion"
}