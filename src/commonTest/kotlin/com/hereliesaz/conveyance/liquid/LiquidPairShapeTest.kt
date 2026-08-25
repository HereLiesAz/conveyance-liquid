package com.hereliesaz.conveyance.liquid

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidPairShapeTest {

    private val density = Density(1f)
    private val size = Size(200f, 200f)
    private val tolerance = 0.5f

    private fun boundsOf(proximity: Float) =
        (LiquidPairShape(proximity).createOutline(size, LayoutDirection.Ltr, density) as Outline.Generic).path.getBounds()

    /**
     * At `proximity = 0` the two drops sit fully apart: separate circles of `0.6 * halfSize`
     * radius, centered `0.30 * width` off the box's own center on either side. Exact math, since
     * [LiquidPairShape] builds these from plain `addOval` calls rather than a sampled/smoothed
     * curve.
     */
    @Test
    fun `proximity 0 places two separate half-size circles apart with no neck`() {
        val bounds = boundsOf(0f)
        assertEquals(-20f, bounds.left, tolerance)
        assertEquals(220f, bounds.right, tolerance)
        assertEquals(40f, bounds.top, tolerance)
        assertEquals(160f, bounds.bottom, tolerance)
    }

    /**
     * At `proximity = 1` the two centers coincide and the radius grows to the full half-size --
     * one circle exactly inscribed in the box, same as if there had only ever been one drop.
     */
    @Test
    fun `proximity 1 merges into a single circle exactly inscribed in the box`() {
        val bounds = boundsOf(1f)
        assertEquals(0f, bounds.left, tolerance)
        assertEquals(200f, bounds.right, tolerance)
        assertEquals(0f, bounds.top, tolerance)
        assertEquals(200f, bounds.bottom, tolerance)
    }

    @Test
    fun `an intermediate proximity sits strictly between the separate and merged extents`() {
        val separate = boundsOf(0f)
        val mid = boundsOf(0.5f)
        val merged = boundsOf(1f)
        assertTrue(mid.width < separate.width, "mid=${mid.width} should be narrower than separate=${separate.width}")
        assertTrue(mid.width > merged.width, "mid=${mid.width} should be wider than merged=${merged.width}")
    }

    @Test
    fun `proximity is coerced into the 0 to 1 range for out-of-range input`() {
        assertEquals(boundsOf(0f), boundsOf(-5f))
        assertEquals(boundsOf(1f), boundsOf(5f))
    }
}
