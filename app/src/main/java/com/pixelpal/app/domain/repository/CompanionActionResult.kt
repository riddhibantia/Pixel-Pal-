package com.pixelpal.app.domain.repository

import com.pixelpal.app.domain.model.Companion

/** Result of an operation that can be blocked by the active-companion limit. */
sealed class CompanionActionResult {
    data class Success(val companion: Companion? = null) : CompanionActionResult()
    object LimitReached : CompanionActionResult()
    data class Error(val message: String? = null) : CompanionActionResult()
}