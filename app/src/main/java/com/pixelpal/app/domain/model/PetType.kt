package com.pixelpal.app.domain.model

enum class PetType(
    val id: String,
    val displayName: String,
    val unlockBondLevel: Int,
    val description: String,
    /** true when every [com.pixelpal.app.animation.AnimationState] has a matching drawable */
    val hasFullAnimationSet: Boolean
) {
    CAT("cat", "Cat", 0, "Friendly & balanced default companion", true),
    DOG("dog", "Dog", 10, "Loyal, playful & energetic friend", false),
    BUNNY("bunny", "Bunny", 25, "Gentle, curious & sleepy pal", false),
    FOX("fox", "Fox", 40, "Clever, confident & independent fox", false),
    AXOLOTL("axolotl", "Axolotl", 60, "Calm, rare & magical companion", false);

    companion object {
        fun fromId(id: String): PetType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CAT
        }
    }
}