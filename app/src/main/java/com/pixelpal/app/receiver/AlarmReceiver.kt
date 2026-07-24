package com.pixelpal.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("AlarmReceiver received intent: ${intent.action}")
        // Reminder trigger logic will be added in Phase 2
    }
}
