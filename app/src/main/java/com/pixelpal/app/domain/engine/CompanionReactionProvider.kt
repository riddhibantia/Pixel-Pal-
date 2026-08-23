package com.pixelpal.app.domain.engine

import com.pixelpal.app.data.dialogue.DialogueLoader
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.Companion
import com.pixelpal.app.domain.model.Emotion
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contextual reaction layer for THE companion. Message categories are driven
 * by live app state (tasks due, agent status, streaks) — never generic spam.
 * GENERAL-style interactions still delegate to the [DialogueLoader] packs,
 * which filter by emotion/bond/personality.
 */
@Singleton
class CompanionReactionProvider @Inject constructor(
    private val dialogueLoader: DialogueLoader,
    private val preferencesManager: PreferencesManager,
    private val personalityEngine: PersonalityEngine
) {
    enum class Interaction { TAP, DOUBLE_TAP, FEED }

    /** Message shown when the user interacts (tap/feed) with the companion. */
    suspend fun interactionMessage(
        companion: Companion,
        bondLevel: Int,
        pendingTaskCount: Int,
        interaction: Interaction
    ): String? {
        val userName = preferencesManager.userName.first().ifEmpty { "friend" }
        val vars = mapOf(
            "name" to companion.name,
            "user" to userName,
            "pet_name" to companion.name,
            "user_name" to userName
        )

        // Occasionally weave in real state instead of a pure reaction.
        if (interaction == Interaction.TAP && pendingTaskCount > 0 &&
            listOf(0, 1).random() == 0
        ) {
            return fill(
                listOf(
                    "You still have $pendingTaskCount task${if (pendingTaskCount > 1) "s" else ""} left today.",
                    "$pendingTaskCount tasks on the list, {user} — we've got this!"
                ),
                vars
            )
        }

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
        ) ?: fallbackInteraction(interaction, vars)
    }

    /** Agent-state-driven message (overlay/workspace feedback). */
    suspend fun agentStateMessage(companion: Companion, state: AgentState): String {
        val userName = preferencesManager.userName.first().ifEmpty { "friend" }
        val vars = mapOf("name" to companion.name, "user" to userName)
        val lines = when (state) {
            AgentState.WORKING -> listOf("Your coding agent is working.", "${companion.name} sees your agent busy at work.", "Agent on it — check back soon.")
            AgentState.WAITING_FOR_INPUT -> listOf("Your agent needs your input!", "The agent is waiting for you, {user}.")
            AgentState.COMPLETED -> listOf("Great news! Your agent finished the task.", "Your agent completed its work.")
            AgentState.ERROR -> listOf("Something went wrong with your agent.", "The build failed — your agent needs attention.")
            AgentState.OFFLINE -> listOf("Your agent is unreachable right now.", "Can't reach your agent's endpoint.")
            AgentState.CONNECTING -> listOf("Connecting to your agent…", "Reaching out to your coding agent.")
            AgentState.ONLINE -> listOf("Your agent is online.", "Connected to your coding agent.")
            AgentState.IDLE -> listOf("Your agent is idle and standing by.", "All quiet on the agent front.")
            AgentState.DISCONNECTED -> listOf("No agent connected yet. Set one up in the workspace!")
        }
        return fill(lines, vars) ?: "${companion.name}: ${state.displayName}"
    }

    private fun fallbackInteraction(
        interaction: Interaction,
        vars: Map<String, String>
    ): String? = fill(
        when (interaction) {
            Interaction.DOUBLE_TAP -> listOf("Yay! High five!", "Double the fun!")
            Interaction.FEED -> listOf("That was tasty!", "Thanks, {user}!")
            Interaction.TAP -> listOf("Hey, again?", "I'm here if you need me!")
        },
        vars
    )

    private fun fill(lines: List<String>, vars: Map<String, String>): String? =
        lines.randomOrNull()?.let { line ->
            vars.entries.fold(line) { acc, (key, value) -> acc.replace("{$key}", value) }
        }
}