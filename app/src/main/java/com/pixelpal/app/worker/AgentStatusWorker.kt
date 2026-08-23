package com.pixelpal.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Periodic agent poll. WorkManager intervals are scheduling hints, not exact
 * timers; the worker re-checks `pollingEnabled` and bails when polling is off.
 */
@HiltWorker
class AgentStatusWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val agentConnectionRepository: AgentConnectionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val companionId = inputData.getLong("companion_id", -1L)
        if (companionId == -1L) return Result.failure()

        val connection = agentConnectionRepository.getConnectionDirect(companionId)
            ?: return Result.success()
        if (!connection.pollingEnabled || connection.endpointUrl.isBlank()) {
            return Result.success()
        }

        return try {
            agentConnectionRepository.checkNow(companionId)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Agent poll failed for companion $companionId")
            Result.retry()
        }
    }
}