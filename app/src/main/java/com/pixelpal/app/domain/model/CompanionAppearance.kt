package com.pixelpal.app.domain.model

/**
 * Foundation for a modular PixelPal companion appearance system.
 *
 * Phase 1: Only the base species is used; all other layers are nullable
 * extension points. The renderer will ignore null layers and fall back to
 * the original cat's baked-in visuals, guaranteeing pixel-identical output.
 *
 * Future phases will populate these layers one at a time:
 *   Base Companion
 *     + Color Layer (baseColor)
 *     + Ear Layer (earStyle)
 *     + Fur Layer (furStyle)
 *     + Eye Layer (eyeStyle)
 *     + Expression Layer (expression)
 *     + Pattern Layer (pattern)
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
                baseColor = null,
                earStyle = null,
                furStyle = null,
                eyeStyle = null,
                expression = null,
                pattern = null
            )

        fun defaultCat(): CompanionAppearance =
            CompanionAppearance(species = "cat")
    }
}
