package com.pixelpal.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pixelpal.app.R
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.engine.CompanionEngine
import com.pixelpal.app.domain.repository.CompanionRepository
import com.pixelpal.app.presentation.MainActivity
import com.pixelpal.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground host for overlay sessions. Owns NO companion identity itself:
 * it resolves the desired set of on-screen companions
 * (master toggle + user selection, defaulting to the active companion) and
 * syncs [OverlayManager] sessions to it. Each session knows its own companion.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var companionEngine: CompanionEngine
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var companionRepository: CompanionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Timber.d("OverlayService created")
        createNotificationChannel()
        startForeground(Constants.FOREGROUND_SERVICE_ID, buildNotification(Constants.DEFAULT_PET_NAME))

        scope.launch {
            if (!Settings.canDrawOverlays(this@OverlayService)) {
                Timber.w("SYSTEM_ALERT_WINDOW permission not granted, skipping overlays")
                stopSelf()
                return@launch
            }

            combine(
                preferencesManager.overlayEnabled,
                companionRepository.getPrimary()
            ) { enabled, companion -> enabled to companion }
                .collect { (enabled, companion) ->
                    val desired = if (enabled && companion != null) {
                        listOf(companion.id)
                    } else {
                        emptyList()
                    }
                    syncSessions(desired)
                    if (companion != null && desired.isNotEmpty()) {
                        notifyForeground(companion.name)
                    }
                }
        }
    }

    private suspend fun syncSessions(desiredIds: List<Long>) {
        // Stop sessions no longer wanted (single-overlay invariant: at most one).
        overlayManager.activeCompanionIds().filter { it !in desiredIds }.forEach {
            overlayManager.hideCompanionFor(it)
        }
        desiredIds.forEach { id ->
            val companion = companionRepository.getByIdDirect(id) ?: return@forEach
            if (!overlayManager.isShowing(id)) {
                overlayManager.showCompanionFor(
                    companionId = companion.id,
                    petType = companion.effectiveSpecies,
                    onTap = { cid -> companionEngine.onTap(cid) },
                    onDoubleTap = { cid -> companionEngine.onDoubleTap(cid) },
                    onLongPress = { cid -> companionEngine.onFeed(cid) }
                )
            } else {
                overlayManager.updatePetTypeFor(id, companion.effectiveSpecies)
            }
        }
    }

    private fun notifyForeground(petNames: String) {
        getSystemService(NotificationManager::class.java)?.notify(
            Constants.FOREGROUND_SERVICE_ID,
            buildNotification(petNames)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("OverlayService destroyed")
        scope.cancel()
        overlayManager.stopAllOverlays()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_COMPANION,
                Constants.CHANNEL_COMPANION_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your pixel companions active on screen"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(petNames: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_COMPANION)
            .setContentTitle(petNames.ifBlank { "PixelPal" })
            .setContentText("$petNames ${if (petNames.contains(',')) "are" else "is"} hanging out with you 🐱")
            .setSmallIcon(R.drawable.ic_stat_companion)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }
}