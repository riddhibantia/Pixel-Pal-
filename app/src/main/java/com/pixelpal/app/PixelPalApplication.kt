package com.pixelpal.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pixelpal.app.data.local.datastore.CompanionBootstrapInitializer
import com.pixelpal.app.domain.engine.ActiveCompanionManager
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.overlay.OverlayManager
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

    @Inject
    lateinit var syncCoordinator: com.pixelpal.app.data.remote.firebase.FirestoreSyncCoordinator

    @Inject
    lateinit var overlayManager: OverlayManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Foreground tracking for the overlay: the floating bubble parks
        // itself while the user is inside the app (it would otherwise run a
        // second infinite Lottie loop on top of the in-app hero).
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var resumed = 0

            override fun onActivityResumed(activity: Activity) {
                resumed++
                overlayManager.setAppForeground(true)
            }

            override fun onActivityPaused(activity: Activity) {
                resumed = (resumed - 1).coerceAtLeast(0)
                if (resumed == 0) overlayManager.setAppForeground(false)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

        workerScheduler.schedulePeriodicWork()

        // Real-time cloud → local listeners (no-op until signed in).
        syncCoordinator.startRealtimeSync()

        // Startup reconciliation: one-time fold of legacy multi-companion data
        // into THE companion, fresh-install seeding, bond/personality guarantees,
        // and restore of any configured agent polling.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                companionBootstrapInitializer.runStartupReconciliation()
                // Returning session (Firebase persists auth across restarts):
                // pull users/{uid} BEFORE the UI reads Room, or Home renders
                // the just-seeded default instead of the real cloud companion.
                // Fresh-seed rows always lose to cloud rows (see coordinator),
                // so this never clobbers a genuinely new user (no cloud yet).
                if (syncCoordinator.isUserLoggedIn) {
                    try {
                        syncCoordinator.onUserAuthenticated()
                    } catch (e: Exception) {
                        Timber.w(e, "Cold-start cloud restore failed")
                    }
                }
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