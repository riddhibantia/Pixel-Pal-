package com.pixelpal.app.domain.model

enum class PetType(
    val id: String,
    val displayName: String,
    val unlockBondLevel: Int,
    val description: String
) {
    CAT("cat", "Cat", 0, "Friendly & balanced default companion"),
    DOG("dog", "Dog", 10, "Loyal, playful & energetic friend"),
    BUNNY("bunny", "Bunny", 25, "Gentle, curious & sleepy pal"),
    FOX("fox", "Fox", 40, "Clever, confident & independent fox"),
    AXOLOTL("axolotl", "Axolotl", 60, "Calm, rare & magical companion");

    companion object {
        fun fromId(id: String): PetType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CAT
        }
    }
}