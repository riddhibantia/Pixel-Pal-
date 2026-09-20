package com.pixelpal.app.domain.model

import kotlinx.serialization.Serializable

/**
 * An action the agent wants to take but must not take without the user.
 * The endpoint advertises it inside the status envelope as
 * `pendingApproval: {id, action, detail?}`; the app notifies once per id
 * and POSTs `{approvalId, decision: "approve"|"deny"}` back.
 */
@Serializable
data class PendingApproval(
    val id: String,
    val action: String,
    val detail: String? = null
)
