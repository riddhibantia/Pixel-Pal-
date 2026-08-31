package com.pixelpal.app.presentation.theme

import androidx.compose.ui.graphics.Color

object CompanionColors {
    val Orange = Color(0xFFFF8A65)
    val Blue = Color(0xFF42A5F5)
    val Purple = Color(0xFFAB47BC)
    val Pink = Color(0xFFEC407A)
    val Green = Color(0xFF66BB6A)

    fun forName(name: String): Color = when (name.lowercase()) {
        "blue" -> Blue
        "purple" -> Purple
        "pink" -> Pink
        "green" -> Green
        else -> Orange
    }
}
