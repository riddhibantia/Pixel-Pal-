package com.pixelpal.app.domain.usecase.companion

import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.model.ActivityType
import com.pixelpal.app.domain.repository.ActivityEventRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.worker.WorkerScheduler
import javax.inject.Inject

class ArchiveCompanionUseCase @Inject constructor(
    private val companionRepository: CompanionRepository,
    private val activeCompanionManager: ActiveCompanionManager,
    private val activityEventRepository: ActivityEventRepository,
    private val workerScheduler: WorkerScheduler
) {
    suspend operator fun invoke(companionId: Long) {
        val companion = companionRepository.getByIdDirect(companionId)
        companionRepository.archive(companionId)
        workerScheduler.cancelAgentPolling(companionId)
        activityEventRepository.record(
            companionId = companionId,
            type = ActivityType.COMPANION_ARCHIVED,
            title = "${companion?.name ?: "Companion"} was archived"
        )
        activeCompanionManager.onCompanionArchived(companionId)
    }
}