package com.pixelpal.app.domain.engine

import com.pixelpal.app.data.dialogue.DialogueLoader
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.CompanionRole
import com.pixelpal.app.domain.model.Emotion
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Role- and personality-aware reaction layer. All companion speech goes
 * through here so no hard-coded strings scatter across UI or services.
 *
 * GENERAL companions delegate to the existing [DialogueLoader] packs (which
 * already filter by emotion/bond/personality); other roles draw from the
 * centralized phrase sets below, with light personalization.
 */
@Singleton
class CompanionReactionProvider @Inject constructor(
    private val dialogueLoader: DialogueLoader,
    private val preferencesManager: PreferencesManager,
    private val personalityEngine: PersonalityEngine
) {
    enum class Interaction { TAP, DOUBLE_TAP, FEED }

    /** Returns the message shown for a direct interaction, or null if none fits. */
    suspend fun interactionMessage(
        companion: Companion,
        bondLevel: Int,
        interaction: Interaction
    ): String? {
        val userName = preferencesManager.userName.first().ifEmpty { "friend" }
        val vars = mapOf(
            "name" to companion.name,
            "user" to userName,
            "pet_name" to companion.name,
            "user_name" to userName
        )

        return when (companion.role) {
            CompanionRole.GENERAL -> generalLine(companion, bondLevel, interaction, vars)
            CompanionRole.TASK -> fill(TASK_LINES[interaction].orEmpty(), vars)
            CompanionRole.REMINDER -> fill(REMINDER_LINES[interaction].orEmpty(), vars)
            CompanionRole.AI_AGENT -> fill(AGENT_IDLE_TAP_LINES, vars)
            CompanionRole.CUSTOM -> fill(CUSTOM_LINES[interaction].orEmpty(), vars)
        }
    }

    /**
     * Message describing an AI-agent state change (used by overlays and the
     * workspace "Check now" flow feedback).
     */
    suspend fun agentStateMessage(companion: Companion, state: AgentState): String {
        val userName = preferencesManager.userName.first().ifEmpty { "friend" }
        val vars = mapOf("name" to companion.name, "user" to userName)
        val lines = when (state) {
            AgentState.WORKING -> listOf("Still working…", "${companion.name} is busy processing.", "Working on it — check back soon.")
            AgentState.WAITING_FOR_INPUT -> listOf("I need your input, {user}!", "Waiting for you — I'm blocked.", "Your turn, {user}!")
            AgentState.COMPLETED -> listOf("Task complete!", "Done! Check the results.", "Finished what you asked for.")
            AgentState.FAILED -> listOf("The check failed — is my endpoint okay?", "Couldn't reach home base.", "Something went wrong on my end.")
            AgentState.OFFLINE -> listOf("I'm offline — check my connection settings.", "Can't reach the network right now.")
            AgentState.CONNECTING -> listOf("Connecting…", "Reaching out to my endpoint.")
            AgentState.IDLE -> listOf("Standing by.", "All quiet on my end.")
            AgentState.STOPPED -> listOf("Stopped. Start me again anytime.")
        }
        return fill(lines, vars) ?: "${companion.name}: ${state.displayName}"
    }

    private suspend fun generalLine(
        companion: Companion,
        bondLevel: Int,
        interaction: Interaction,
        vars: Map<String, String>
    ): String? {
        val personality = personalityEngine.getPersonalityDirect(companion.id)
        val emotion = when (interaction) {
            Interaction.DOUBLE_TAP -> Emotion.EXCITED
            else -> Emotion.HAPPY
        }
        val context = when (interaction) {
            Interaction.TAP -> "tap_response"
            Interaction.DOUBLE_TAP -> "double_tap_response"
            Interaction.FEED -> "feed_response"
        }
        return dialogueLoader.getLine(
            contextStr = context,
            emotion = emotion,
            bondLevel = bondLevel,
            personality = personality,
            variables = vars
        )
    }

    private fun fill(lines: List<String>, vars: Map<String, String>): String? =
        lines.randomOrNull()?.let { line ->
            vars.entries.fold(line) { acc, (key, value) -> acc.replace("{$key}", value) }
        }

    private val TASK_LINES = mapOf(
        Interaction.TAP to listOf(
            "Let's crush the next task, {user}!",
            "Checklist time — what's next?",
            "One step at a time. I'm on it!",
            "Open your checklist whenever you're ready."
        ),
        Interaction.DOUBLE_TAP to listOf(
            "Double motivation!",
            "We're on a roll, {user}!",
            "That's the spirit!"
        ),
        Interaction.FEED to listOf(
            "Fuel up, then back to work!",
            "Break earned. Back to the checklist after!",
            "Recharging for the next task!"
        )
    )

    private val REMINDER_LINES = mapOf(
        Interaction.TAP to listOf(
            "All reminders on track!",
            "I'll keep watch on the clock, {user}.",
            "Nothing slips past me.",
            "Next reminder will pop up soon."
        ),
        Interaction.DOUBLE_TAP to listOf(
            "Right on schedule!",
            "Clockwork, {user}."
        ),
        Interaction.FEED to listOf(
            "Thanks! I'll stay sharp for your reminders.",
            "Quick snack, then back to guarding the clock."
        )
    )

    private val AGENT_IDLE_TAP_LINES = listOf(
        "Standing by.",
        "All systems normal.",
        "Ping me from my workspace for a status check."
    )

    private val CUSTOM_LINES = mapOf(
        Interaction.TAP to listOf(
            "At your service, {user}!",
            "{name} reporting in.",
            "Happy to help however you configured me!"
        ),
        Interaction.DOUBLE_TAP to listOf(
            "Twice the hello!",
            "You found my secret handshake."
        ),
        Interaction.FEED to listOf(
            "Much appreciated!",
            "A snack for good work."
        )
    )
}