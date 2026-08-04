package com.pixelpal.app.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.media.RingtoneManager
import com.pixelpal.app.domain.engine.CompanionEngine
import com.pixelpal.app.domain.repository.ReminderRepository
import com.pixelpal.app.presentation.MainActivity
import com.pixelpal.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.pixelpal.app.domain.model.Reminder

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var companionEngine: CompanionEngine
    @Inject lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        Timber.d("AlarmReceiver triggered for reminderId: $reminderId")

        if (reminderId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminder = reminderRepository.getById(reminderId)
                    if (reminder != null) {
                        // Show system notification
                        showNotification(context, reminder)
                        
                        // Show speech bubble
                        companionEngine.onReminderTriggered(reminder)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, reminder: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.w("POST_NOTIFICATIONS permission missing; skipping notification for ${reminder.id}")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                Constants.CHANNEL_REMINDER,
                Constants.CHANNEL_REMINDER_NAME,
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Play sound manually because channels cache the sound
        try {
            val uri = reminder.soundUri?.let { android.net.Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()

            // Stop ringtone after 5 seconds to avoid annoying the user if they don't tap
            CoroutineScope(Dispatchers.Main).launch {
                delay(5000)
                if (ringtone?.isPlaying == true) ringtone.stop()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error playing reminder ringtone")
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, Constants.CHANNEL_REMINDER)
            .setSmallIcon(com.pixelpal.app.R.drawable.ic_stat_companion)
            .setContentTitle("PixelPal Reminder: ${reminder.title}")
            .setContentText("It's time!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    reminder.id.toInt(),
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        notificationManager.notify(reminder.id.toInt(), notification)
    }
}
