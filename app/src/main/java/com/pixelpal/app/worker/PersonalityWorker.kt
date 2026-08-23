package com.pixelpal.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelpal.app.domain.engine.DailyInteractionStats
import com.pixelpal.app.domain.engine.PersonalityEngine
import com.pixelpal.app.domain.repository.BondRepository
import com.pixelpal.app.domain.repository.CompanionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class PersonalityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val personalityEngine: PersonalityEngine,
    private val bondRepository: BondRepository,
    private val companionRepository: CompanionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            companionRepository.getAllActiveDirect().forEach { companion ->
                val bond = bondRepository.getBondDirect(companion.id)
                val stats = DailyInteractionStats(
                    tapCount = bond.tapsToday,
                    feedCount = bond.feedsToday,
                    remindersCompleted = 1,
                    remindersIgnored = 0,
                    isLateNightActive = false,
                    isMorningActive = true
                )
                personalityEngine.recalculateDaily(companion.id, stats)
            }
            Timber.d("Personality recalculated successfully by WorkManager")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error recalculating personality in worker")
            Result.failure()
        }
    }
}