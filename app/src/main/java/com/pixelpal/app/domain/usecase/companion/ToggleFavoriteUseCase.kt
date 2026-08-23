package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.repository.CompanionRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val companionRepository: CompanionRepository
) {
    suspend operator fun invoke(companionId: Long, favorite: Boolean) {
        companionRepository.setFavorite(companionId, favorite)
    }
}