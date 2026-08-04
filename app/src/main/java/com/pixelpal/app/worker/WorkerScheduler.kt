package com.pixelpal.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
    }

    companion object {
        private const val BOND_DECAY_WORK_NAME = "pixelpal_bond_decay"
        private const val PERSONALITY_WORK_NAME = "pixelpal_personality"
    }
}