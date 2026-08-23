package com.pixelpal.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pixelpal.app.R
import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun notify(companionId: Long, companionName: String, result: AgentCheckResult) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return

        ensureChannel(manager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            companionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = result.message?.let { " — $it" }.orEmpty()
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_AGENT)
            .setSmallIcon(R.drawable.ic_stat_companion)
            .setContentTitle("$companionName needs attention")
            .setContentText("${result.state.displayName}$message")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(Constants.NOTIFICATION_ID_AGENT + companionId.toInt(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_AGENT,
                Constants.CHANNEL_AGENT_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Status updates from your AI agent companions"
            }
            manager.createNotificationChannel(channel)
        }
    }
}