package com.pixelpal.app.domain.model

/**
 * Pure-appearance transformation of the single companion.
 * Changing these never touches bond/tasks/reminders/agent data.
 */
data class SpeciesStyle(
    val species: String,
    val color: String,
    val pattern: String
) {
    companion object {
        val SPECIES = listOf("cat", "dog", "rabbit", "whale", "llama")
        val COLORS = listOf("orange", "blue", "purple", "pink", "green")
        val PATTERNS = listOf("plain", "stripes", "spots", "patches")

        fun defaults(species: String) = SpeciesStyle(
            species = species,
            color = "orange",
            pattern = "plain"
        )
    }
}