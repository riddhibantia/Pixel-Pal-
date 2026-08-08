package com.pixelpal.app.util

object Constants {
    const val APP_NAME = "PixelPal"
    const val PACKAGE_NAME = "com.pixelpal.app"
    const val DEFAULT_PET_TYPE = "cat"

    // DataStore keys
    const val PREFERENCES_NAME = "pixelpal_preferences"
    const val KEY_OVERLAY_X = "overlay_x"
    const val KEY_OVERLAY_Y = "overlay_y"
    const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    const val KEY_SELECTED_PET_TYPE = "selected_pet_type"
    const val KEY_PET_NAME = "pet_name"
    const val KEY_USER_NAME = "user_name"
    const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    const val KEY_CURRENT_THEME = "current_theme"

    // Notification channels
    const val CHANNEL_COMPANION = "companion_channel"
    const val CHANNEL_REMINDER = "reminder_channel"
    const val CHANNEL_COMPANION_NAME = "Companion"
    const val CHANNEL_REMINDER_NAME = "Reminders"

    // Overlay defaults
    const val OVERLAY_SIZE_DP = 64f
    const val OVERLAY_OFFSET_X_DP = 80f
    const val OVERLAY_OFFSET_Y_DP = 200f
    const val OVERLAY_PILL_TOP_DP = 32f
    const val KEYBOARD_DODGE_THRESHOLD_DP = 100f

    // Default values
    const val DEFAULT_OVERLAY_ENABLED = true
    const val DEFAULT_SELECTED_PET_TYPE = "cat"
    const val DEFAULT_PET_NAME = "PixelPal"
    const val DEFAULT_USER_NAME = ""
    const val DEFAULT_IS_FIRST_LAUNCH = true
    const val DEFAULT_CURRENT_THEME = "dark"

    // Foreground service
    const val FOREGROUND_SERVICE_ID = 1001
    const val NOTIFICATION_ID_COMPANION = 1002
    const val NOTIFICATION_ID_REMINDER = 1003

    // Notification action intents
    const val ACTION_COMPLETE_REMINDER = "com.pixelpal.app.action.COMPLETE_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.pixelpal.app.action.SNOOZE_REMINDER"

    // Room database
    const val DATABASE_NAME = "pixelpal_database_v2"
    const val DATABASE_VERSION = 3
}