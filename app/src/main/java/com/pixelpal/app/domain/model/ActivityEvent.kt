package com.pixelpal.app.domain.model

enum class ActivityType(val id: String) {
    COMPANION_CREATED("COMPANION_CREATED"),
    TAP("TAP"),
    FEED("FEED"),
    BOND_LEVEL_UP("BOND_LEVEL_UP"),
    STREAK_MILESTONE("STREAK_MILESTONE"),
    REMINDER_COMPLETED("REMINDER_COMPLETED"),
    TASK_ADDED("TASK_ADDED"),
    TASK_COMPLETED("TASK_COMPLETED"),
    AGENT_CONNECTED("AGENT_CONNECTED"),
    AGENT_CHECKED("AGENT_CHECKED"),
    AGENT_STATUS_CHANGED("AGENT_STATUS_CHANGED"),
    AGENT_COMMAND_SENT("AGENT_COMMAND_SENT"),
    COMPANION_ARCHIVED("COMPANION_ARCHIVED"),
    COMPANION_RESTORED("COMPANION_RESTORED");

    companion object {
        fun fromId(id: String): ActivityType =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: COMPANION_CREATED
    }
}

/**
 * One entry on a companion's timeline. Powers the in-app Activity Center.
 * Ordinary taps/feeds are intentionally NOT recorded here — they only mutate
 * bond/emotion. [ActivityType.TAP]/[ActivityType.FEED] remain for legacy rows,
 * which the Activity Center query excludes.
 */
data class ActivityEvent(
    val id: Long = 0,
    val companionId: Long,
    val type: ActivityType = ActivityType.COMPANION_CREATED,
    val title: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)