package com.pixelpal.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.pixelpal.app.R
import com.pixelpal.app.animation.AnimationEngine
import com.pixelpal.app.animation.SpriteAnimator
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.engine.CompanionEngine
import com.pixelpal.app.presentation.MainActivity
import com.pixelpal.app.receiver.ScreenStateReceiver
import com.pixelpal.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var animationEngine: AnimationEngine
    @Inject lateinit var spriteAnimator: SpriteAnimator
    @Inject lateinit var companionEngine: CompanionEngine
    @Inject lateinit var preferencesManager: PreferencesManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var screenStateReceiver: ScreenStateReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("OverlayService created")
        createNotificationChannel()
        registerScreenStateReceiver()

        startForeground(Constants.FOREGROUND_SERVICE_ID, buildNotification(Constants.DEFAULT_PET_NAME))

        scope.launch {
            val petName = preferencesManager.petName.first()
            val petType = preferencesManager.selectedPetType.first()

            spriteAnimator.setPetType(petType)
            animationEngine.initialize()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(Constants.FOREGROUND_SERVICE_ID, buildNotification(petName))

            if (!Settings.canDrawOverlays(this@OverlayService)) {
                Timber.w("SYSTEM_ALERT_WINDOW permission not granted, skipping overlay")
                return@launch
            }

            overlayManager.showCompanion(
                onTap = { companionEngine.onTap() },
                onDoubleTap = { companionEngine.onDoubleTap() },
                onLongPress = { companionEngine.onFeed() }
            )

            launch {
                spriteAnimator.currentDrawableRes.collect { resId ->
                    if (resId != 0) {
                        overlayManager.updateSprite(resId)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("OverlayService destroyed")
        unregisterScreenStateReceiver()
        animationEngine.destroy()
        overlayManager.hideCompanion()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerScreenStateReceiver() {
        screenStateReceiver = ScreenStateReceiver(
            onScreenOn = {
                animationEngine.setScreenOn(true)
                animationEngine.initialize()
            },
            onScreenOff = {
                animationEngine.setScreenOn(false)
            }
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    private fun unregisterScreenStateReceiver() {
        screenStateReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // ignore if already unregistered
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_COMPANION,
                Constants.CHANNEL_COMPANION_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your pixel companion active on screen"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(petName: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_COMPANION)
            .setContentTitle(petName)
            .setContentText("$petName is hanging out with you 🐱")
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
                context.startForegroundService(intent)
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
