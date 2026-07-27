package com.pixelpal.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class ScreenStateReceiver(
    private val onScreenOn: () -> Unit = {},
    private val onScreenOff: () -> Unit = {}
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Timber.d("Screen OFF -> Pausing animation timers to save battery")
                onScreenOff()
            }
            Intent.ACTION_SCREEN_ON -> {
                Timber.d("Screen ON -> Resuming companion animation timers")
                onScreenOn()
            }
        }
    }
}
