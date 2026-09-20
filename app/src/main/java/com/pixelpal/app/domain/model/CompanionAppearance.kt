package com.pixelpal.app.domain.model

/**
 * Foundation for a modular PixelPal companion appearance system.
 *
 * The renderer is Lottie-first (`res/raw/pet_{species}_{state}.json`, one full
 * 12-state set per species); color/pattern travel with the companion and tint
 * the surrounding UI (preview glow, hero aura) while body recolor stays a
 * future layer. Null layers fall back to the baked-in Lottie visuals.
 */
data class CompanionAppearance(
    val species: String,
    /** Null = use the drawable's original hardcoded fill. */
    val baseColor: String? = null,
    val earStyle: String? = null,
    val furStyle: String? = null,
    val eyeStyle: String? = null,
    val expression: String? = null,
    val pattern: String? = null
) {
    companion object {
        /** From the persisted single companion row. */
        fun fromCompanion(companion: com.pixelpal.app.domain.model.Companion): CompanionAppearance =
            CompanionAppearance(
                species = companion.effectiveSpecies,
                baseColor = companion.color,
                earStyle = null,
                furStyle = null,
                eyeStyle = null,
                expression = null,
                pattern = companion.pattern
            )

        fun defaultCat(): CompanionAppearance =
            CompanionAppearance(species = "cat")
    }
}
