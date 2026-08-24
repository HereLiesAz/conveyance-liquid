package com.hereliesaz.conveyance.liquid

import androidx.compose.ui.graphics.Color

/**
 * A liquid's tint. Real mercury is silver -- [mercury] is the default -- but nothing about
 * surface-tension physics requires a specific color, so a handful of tinted variants exist for a
 * host that wants a colored liquid. Each entry is a `base`/`highlight`/`shadow` triad so a drop
 * can be rendered as a glossy sphere (a bright, off-center specular highlight over a darker rim),
 * not a flat fill -- the reflectivity is part of what reads as "liquid" rather than "circle."
 */
data class LiquidTint(val base: Color, val highlight: Color, val shadow: Color)

object LiquidHue {
    val mercury = LiquidTint(Color(0xFFB8BCC0), Color(0xFFF2F4F6), Color(0xFF6E7276))
    val azure = LiquidTint(Color(0xFF3E7EB8), Color(0xFF9BC9EC), Color(0xFF1E3F5C))
    val verdant = LiquidTint(Color(0xFF3E8B5C), Color(0xFF9BD9B4), Color(0xFF1E4630))
    val ember = LiquidTint(Color(0xFFC4562E), Color(0xFFF2A97C), Color(0xFF6E2A12))
    val violet = LiquidTint(Color(0xFF6E4FA8), Color(0xFFC4A9EC), Color(0xFF3A2758))

    private val named = listOf(mercury, azure, verdant, ember, violet)

    /** Looks up a tint by the composable manifest's `hue` string; an unrecognized id is hashed onto one of [named]. */
    fun of(hue: String): LiquidTint = when (hue) {
        "mercury" -> mercury
        "azure" -> azure
        "verdant" -> verdant
        "ember" -> ember
        "violet" -> violet
        else -> named[(if (hue.hashCode() < 0) -hue.hashCode() else hue.hashCode()) % named.size]
    }
}
