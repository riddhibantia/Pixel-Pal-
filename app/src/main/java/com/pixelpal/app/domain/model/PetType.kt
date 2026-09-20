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
    DOG("dog", "Dog", 0, "Loyal, playful & energetic friend", true),
    BUNNY("bunny", "Bunny", 0, "Gentle, curious & sleepy pal", true),
    PANDA("panda", "Panda", 0, "Gentle, cuddly & bamboo-loving buddy", true),
    WHALE("whale", "Whale", 0, "Calm, deep & dreamy swimmer", true),
    AXOLOTL("axolotl", "Axolotl", 0, "Calm, rare & magical companion", true),
    LLAMA("llama", "Llama", 0, "Chill, quirky & steadfast pal", true);

    companion object {
        fun fromId(id: String): PetType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CAT
        }
    }
}