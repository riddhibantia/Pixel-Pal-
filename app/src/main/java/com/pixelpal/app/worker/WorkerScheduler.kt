package com.pixelpal.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pixelpal.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedulePeriodicWork() {
        val bondDecayRequest = PeriodicWorkRequestBuilder<BondDecayWorker>(1, TimeUnit.DAYS)
            .build()
        val personalityRequest = PeriodicWorkRequestBuilder<PersonalityWorker>(1, TimeUnit.DAYS)
            .build()
        val firestoreSyncRequest = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BOND_DECAY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            bondDecayRequest
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERSONALITY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            personalityRequest
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FIRESTORE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            firestoreSyncRequest
        )
    }

    fun triggerImmediateSync() {
        val request = androidx.work.OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Unique periodic polling for one agent companion. Network-constrained and
     * keyed by companion id so re-saving config never duplicates work.
     */
    fun scheduleAgentPolling(companionId: Long, intervalMinutes: Long) {
        // WorkManager enforces a ~15-minute minimum for periodic work.
        val interval = intervalMinutes.coerceAtLeast(Constants.DEFAULT_AGENT_POLL_INTERVAL_MIN)
        val request = PeriodicWorkRequestBuilder<AgentStatusWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(workDataOf("companion_id" to companionId))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            agentWorkName(companionId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAgentPolling(companionId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(agentWorkName(companionId))
    }

    private fun agentWorkName(companionId: Long) = Constants.AGENT_WORK_PREFIX + companionId

    companion object {
        private const val BOND_DECAY_WORK_NAME = "pixelpal_bond_decay"
        private const val PERSONALITY_WORK_NAME = "pixelpal_personality"
        private const val FIRESTORE_SYNC_WORK_NAME = "pixelpal_firestore_sync"
    }
}