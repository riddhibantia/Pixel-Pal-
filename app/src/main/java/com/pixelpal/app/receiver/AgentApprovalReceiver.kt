package com.pixelpal.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelpal.app.domain.repository.AgentConnectionRepository
import com.pixelpal.app.util.AgentNotificationHelper
import com.pixelpal.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Answers agent approval gates from the notification shade: Approve/Deny
 * POSTs `{approvalId, decision}` to the agent and dismisses the ask.
 */
@AndroidEntryPoint
class AgentApprovalReceiver : BroadcastReceiver() {

    @Inject lateinit var agentConnectionRepository: AgentConnectionRepository
    @Inject lateinit var agentNotificationHelper: AgentNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val companionId = intent.getLongExtra(EXTRA_COMPANION_ID, -1L)
        val approvalId = intent.getStringExtra(EXTRA_APPROVAL_ID).orEmpty()
        if (companionId == -1L || approvalId.isBlank()) return
        val approved = intent.action == Constants.ACTION_APPROVE_AGENT

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = agentConnectionRepository.respondToApproval(companionId, approvalId, approved)
                if (result.isFailure) {
                    Timber.w("Approval response failed: ${result.exceptionOrNull()?.message}")
                }
                agentNotificationHelper.cancelApproval(companionId, approvalId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_COMPANION_ID = "companion_id"
        const val EXTRA_APPROVAL_ID = "approval_id"
    }
}
