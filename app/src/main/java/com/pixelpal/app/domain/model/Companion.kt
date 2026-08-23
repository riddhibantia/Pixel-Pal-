package com.pixelpal.app.domain.model

/**
 * The SINGLE user companion. Exactly one active record exists.
 * Tasks/reminders/bond/personality/activity/agent are FEATURES of this
 * companion, not separate companions.
 */
data class Companion(
    val id: Long = 0,
    val name: String = "Pixel",
    /** Species drives the sprite family (cat/dog/rabbit/whale/llama). */
    val petType: String = "cat",
    val role: CompanionRole = CompanionRole.GENERAL,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val hatId: String? = null,
    val outfitId: String? = null,
    val accessoryId: String? = null,
    /** Pure appearance — transformation never touches bond/tasks/reminders. */
    val species: String = "cat",
    val color: String = "orange",
    val pattern: String = "plain"
) {
    /** Effective species for sprite lookup (species overrides legacy petType). */
    val effectiveSpecies: String get() = species.ifBlank { petType }
}