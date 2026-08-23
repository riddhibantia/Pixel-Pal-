package com.pixelpal.app.domain.model

/**
 * Companion identity: what the companion IS, independent of any runtime state.
 * Runtime operational state (e.g. AI agent activity) lives in the agent domain.
 */
enum class CompanionRole(
    val id: String,
    val displayName: String,
    val description: String
) {
    GENERAL("GENERAL", "General", "An everyday companion that hangs out, plays and grows with you."),
    REMINDER("REMINDER", "Reminder", "Keeps you on schedule and helps you remember the important stuff."),
    TASK("TASK", "Task", "A task companion for your to-do checklist."),
    AI_AGENT("AI_AGENT", "AI Agent", "A monitored AI agent whose status you can watch from the workspace."),
    CUSTOM("CUSTOM", "Custom", "A companion you define your own way.");

    companion object {
        fun fromId(id: String): CompanionRole =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: GENERAL
    }
}