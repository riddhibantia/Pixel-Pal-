package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.CompanionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCompanionsUseCase @Inject constructor(
    private val companionRepository: CompanionRepository
) {
    fun getAllActive(): Flow<List<Companion>> = companionRepository.getAllActive()
    fun getAll(): Flow<List<Companion>> = companionRepository.getAll()
    fun getArchived(): Flow<List<Companion>> = companionRepository.getArchived()
    fun getById(id: Long): Flow<Companion?> = companionRepository.getById(id)
}