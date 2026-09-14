package com.pixelpal.app.domain.model

enum class AvatarSlot { EYES, EARS, HEADWEAR, SCARF, AURA }

data class AvatarOption(
    val id: String,
    val slot: AvatarSlot,
    val displayName: String,
    val unlockBondLevel: Int
)

object AvatarOptions {
    val all: List<AvatarOption> = listOf(
        AvatarOption("classic", AvatarSlot.EYES, "Classic", 0),
        AvatarOption("happy-starry", AvatarSlot.EYES, "Starry", 0),
        AvatarOption("sleepy", AvatarSlot.EYES, "Sleepy", 0),
        AvatarOption("wink", AvatarSlot.EYES, "Wink", 0),
        AvatarOption("pointy-cat", AvatarSlot.EARS, "Pointy", 0),
        AvatarOption("floppy", AvatarSlot.EARS, "Floppy", 0),
        AvatarOption("round", AvatarSlot.EARS, "Round", 0),
        AvatarOption("none-headwear", AvatarSlot.HEADWEAR, "None", 0),
        AvatarOption("bow", AvatarSlot.HEADWEAR, "Bow", 5),
        AvatarOption("cap", AvatarSlot.HEADWEAR, "Cap", 5),
        AvatarOption("crown", AvatarSlot.HEADWEAR, "Crown", 5),
        AvatarOption("none-scarf", AvatarSlot.SCARF, "None", 0),
        AvatarOption("red-scarf", AvatarSlot.SCARF, "Red", 10),
        AvatarOption("teal-scarf", AvatarSlot.SCARF, "Teal", 10),
        AvatarOption("none-aura", AvatarSlot.AURA, "None", 0),
        AvatarOption("gold-sparkle", AvatarSlot.AURA, "Gold", 15),
        AvatarOption("pink-glow", AvatarSlot.AURA, "Pink", 15)
    )

    fun forSlot(slot: AvatarSlot): List<AvatarOption> = all.filter { it.slot == slot }

    fun fromId(id: String?): AvatarOption? = id?.let { want -> all.find { it.id == want } }

    fun fallbackFor(slot: AvatarSlot): AvatarOption = when (slot) {
        AvatarSlot.EYES -> fromId("classic")!!
        AvatarSlot.EARS -> fromId("pointy-cat")!!
        AvatarSlot.HEADWEAR -> fromId("none-headwear")!!
        AvatarSlot.SCARF -> fromId("none-scarf")!!
        AvatarSlot.AURA -> fromId("none-aura")!!
    }

    fun isUnlocked(option: AvatarOption, bondLevel: Int): Boolean = bondLevel >= option.unlockBondLevel
}
