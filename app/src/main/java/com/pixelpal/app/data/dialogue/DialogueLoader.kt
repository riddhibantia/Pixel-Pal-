package com.pixelpal.app.data.dialogue

import android.content.Context
import com.pixelpal.app.R
import com.pixelpal.app.domain.model.Emotion
import com.pixelpal.app.domain.model.Personality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DialogueLine(
    val id: String,
    val text: String,
    val emotion: String,
    val context: String,
    val minBond: Int = 0,
    val maxBond: Int = 100,
    val personality: String? = null
)

@Serializable
data class DialoguePack(
    val lines: List<DialogueLine>
)

@Singleton
class DialogueLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val allLines = mutableListOf<DialogueLine>()
    private val recentlyUsed = LinkedList<String>()
    private val maxRecent = 20

    init {
        loadAllDialogue()
    }

    private fun loadAllDialogue() {
        val rawResIds = listOf(
            R.raw.dialogue_reminders,
            R.raw.dialogue_reactions,
            R.raw.dialogue_greetings,
            R.raw.dialogue_general,
            R.raw.dialogue_bond
        )

        for (resId in rawResIds) {
            try {
                val jsonString = context.resources.openRawResource(resId)
                    .bufferedReader().use { it.readText() }
                val pack = json.decodeFromString<DialoguePack>(jsonString)
                allLines.addAll(pack.lines)
            } catch (e: Exception) {
                Timber.e(e, "Error loading dialogue resource $resId")
            }
        }
    }

    fun getLine(
        contextStr: String,
        emotion: Emotion,
        bondLevel: Int,
        personality: Personality? = null,
        variables: Map<String, String> = emptyMap()
    ): String? {
        val candidates = allLines.filter { line ->
            line.context == contextStr &&
            line.emotion.equals(emotion.name, ignoreCase = true) &&
            bondLevel >= line.minBond && bondLevel <= line.maxBond &&
            !recentlyUsed.contains(line.id)
        }

        if (candidates.isEmpty()) {
            val fallbackCandidates = allLines.filter { line -> line.context == contextStr }
            if (fallbackCandidates.isEmpty()) return null
            val selected = fallbackCandidates.random()
            return replaceVariables(selected.text, variables)
        }

        val selected = candidates.random()
        recentlyUsed.add(selected.id)
        if (recentlyUsed.size > maxRecent) {
            recentlyUsed.removeFirst()
        }

        return replaceVariables(selected.text, variables)
    }

    private fun replaceVariables(text: String, vars: Map<String, String>): String {
        var result = text
        vars.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}
