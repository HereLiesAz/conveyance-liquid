package com.hereliesaz.conveyance.liquid

import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the two pieces of [LiquidField]'s neck that are pure arithmetic rather than a `DrawScope`
 * call: which pairs of drops get a neck at all, and what color it is at each end. The second is
 * the defect the audit found and the README documents as fixed -- the neck painted the *first*
 * drop's tint at both of its ends, ignoring the second drop's hue entirely.
 */
class LiquidFieldTest {

    // Two drops of a `drop` surface: 48.dp diameter, so 24 "px" radius at density 1.
    private val radius = 24f
    private val touching = radius * 2f

    @Test
    fun `no neck at all once the drops are further apart than the threshold`() {
        assertNull(liquidNeckCloseness(touching * NECK_THRESHOLD_FACTOR, radius, radius))
        assertNull(liquidNeckCloseness(touching * NECK_THRESHOLD_FACTOR + 1f, radius, radius))
        assertNull(liquidNeckCloseness(1000f, radius, radius))
    }

    @Test
    fun `closeness runs 0 at the threshold to 1 at contact and stays saturated past it`() {
        val atThreshold = liquidNeckCloseness(touching * NECK_THRESHOLD_FACTOR - 0.01f, radius, radius)!!
        assertTrue(atThreshold < 0.01f, "just inside the threshold should be a barely-there neck, was $atThreshold")

        assertEquals(1f, liquidNeckCloseness(touching, radius, radius)!!, 1e-4f)
        // Overlapping drops don't get a neck thicker than a touching pair's.
        assertEquals(1f, liquidNeckCloseness(touching / 2f, radius, radius)!!, 1e-4f)
        assertEquals(1f, liquidNeckCloseness(0f, radius, radius)!!, 1e-4f)
    }

    /**
     * Proximity is relative to the drops' own combined radii, not a flat pixel distance -- the
     * same center separation reads as "nearly touching" for two puddles and "far apart" for two
     * beads.
     */
    @Test
    fun `the threshold scales with the drops' own sizes rather than being a flat pixel count`() {
        val separation = 100f
        val beadRadius = 16f // 32.dp bead
        val puddleRadius = 36f // 72.dp puddle
        assertNull(liquidNeckCloseness(separation, beadRadius, beadRadius))
        assertTrue(liquidNeckCloseness(separation, puddleRadius, puddleRadius) != null)
    }

    /**
     * The regression itself: each end of the neck carries its own drop's highlight, so a neck
     * between two differently-hued drops shows both hues.
     */
    @Test
    fun `each end of the neck carries its own drop's tint`() {
        val stops = liquidNeckGradientStops(LiquidHue.azure, LiquidHue.ember)
        assertEquals(listOf(0f, 0.5f, 1f), stops.map { it.first })
        assertEquals(LiquidHue.azure.highlight, stops.first().second)
        assertEquals(LiquidHue.ember.highlight, stops.last().second)
        assertNotEquals(
            stops.first().second,
            stops.last().second,
            "a neck between two different hues must not paint the same color at both ends",
        )
    }

    @Test
    fun `reversing the two drops reverses the gradient rather than changing it`() {
        val forward = liquidNeckGradientStops(LiquidHue.azure, LiquidHue.ember)
        val backward = liquidNeckGradientStops(LiquidHue.ember, LiquidHue.azure)
        assertEquals(forward.first().second, backward.last().second)
        assertEquals(forward.last().second, backward.first().second)
        assertEquals(forward[1].second, backward[1].second)
    }

    @Test
    fun `the middle is the two drops' shadows mixed evenly, so neither end owns it`() {
        val stops = liquidNeckGradientStops(LiquidHue.azure, LiquidHue.ember)
        assertEquals(lerp(LiquidHue.azure.shadow, LiquidHue.ember.shadow, 0.5f), stops[1].second)
        assertNotEquals(LiquidHue.azure.shadow, stops[1].second)
        assertNotEquals(LiquidHue.ember.shadow, stops[1].second)
    }

    @Test
    fun `two drops of the same hue still read as that one hue end to end`() {
        val stops = liquidNeckGradientStops(LiquidHue.mercury, LiquidHue.mercury)
        assertEquals(LiquidHue.mercury.highlight, stops.first().second)
        assertEquals(LiquidHue.mercury.highlight, stops.last().second)
        // Mixing a shadow with itself is that shadow -- within the rounding of Color.lerp's own
        // perceptual-space round trip, which is not required to be bit-exact.
        val middle = stops[1].second
        val expected = LiquidHue.mercury.shadow
        assertTrue(
            abs(middle.red - expected.red) < 0.01f &&
                abs(middle.green - expected.green) < 0.01f &&
                abs(middle.blue - expected.blue) < 0.01f,
            "expected the middle of a same-hue neck to be that hue's shadow, was $middle",
        )
    }
}
