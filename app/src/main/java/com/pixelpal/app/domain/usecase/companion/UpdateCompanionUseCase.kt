package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.repository.CompanionRepository
import javax.inject.Inject

class UpdateCompanionUseCase @Inject constructor(
    private val companionRepository: CompanionRepository
) {
    suspend operator fun invoke(companion: Companion) {
        companionRepository.update(companion)
    }
}