package com.pixelpal.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pixelpal.app.data.local.datastore.CompanionBootstrapInitializer
import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.repository.AgentConfigRepository
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
    lateinit var agentConfigRepository: AgentConfigRepository

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

        // Idempotent companion bootstrap: seeds the migrated/legacy pet as the
        // default companion, ensures bond/personality rows, and repairs the
        // active selection. Runs once per install.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                companionBootstrapInitializer.ensureInitialized()
                activeCompanionManager.ensureValidActiveCompanion()
                // Restore polling for any agents already configured as enabled.
                agentConfigRepository.getEnabledDirect().forEach { config ->
                    workerScheduler.scheduleAgentPolling(config.companionId, config.pollIntervalMinutes)
                }
            } catch (e: Exception) {
                Timber.e(e, "Companion bootstrap failed")
            }
        }
    }
}