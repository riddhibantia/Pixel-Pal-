package com.pixelpal.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.overlay.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("BootReceiver received BOOT_COMPLETED")
            CoroutineScope(Dispatchers.IO).launch {
                val enabled = preferencesManager.overlayEnabled.first()
                if (enabled) {
                    OverlayService.start(context)
                }
            }
        }
    }
}
