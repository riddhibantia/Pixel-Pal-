package com.pixelpal.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelpal.app.domain.engine.BondEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BondDecayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val bondEngine: BondEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            bondEngine.applyDecay()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
