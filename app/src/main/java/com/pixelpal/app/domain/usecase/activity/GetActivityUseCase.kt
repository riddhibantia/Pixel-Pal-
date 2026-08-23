package com.pixelpal.app.domain.usecase.activity

import com.pixelpal.app.domain.model.ActivityEvent
import com.pixelpal.app.domain.repository.ActivityEventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActivityUseCase @Inject constructor(
    private val activityEventRepository: ActivityEventRepository
) {
    fun forCompanion(companionId: Long, limit: Int = 50): Flow<List<ActivityEvent>> =
        activityEventRepository.getForCompanion(companionId, limit)

    fun recent(limit: Int = 20): Flow<List<ActivityEvent>> =
        activityEventRepository.getRecent(limit)
}