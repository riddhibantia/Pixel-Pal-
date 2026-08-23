package com.pixelpal.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pixelpal.app.data.local.datastore.CompanionBootstrapInitializer
import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.worker.WorkerScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PixelPalApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    @Inject
    lateinit var companionBootstrapInitializer: CompanionBootstrapInitializer

    @Inject
    lateinit var activeCompanionManager: ActiveCompanionManager

    @Inject
    lateinit var agentConnectionRepository: AgentConnectionRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        workerScheduler.schedulePeriodicWork()

        // Startup reconciliation: one-time fold of legacy multi-companion data
        // into THE companion, fresh-install seeding, bond/personality guarantees,
        // and restore of any configured agent polling.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                companionBootstrapInitializer.runStartupReconciliation()
                activeCompanionManager.ensureValidActiveCompanion()
                // Restore polling for the companion's agent when configured.
                agentConnectionRepository.getPollingEnabledDirect().forEach { connection ->
                    workerScheduler.scheduleAgentPolling(
                        connection.companionId,
                        connection.pollingIntervalMinutes
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Companion bootstrap failed")
            }
        }
    }
}