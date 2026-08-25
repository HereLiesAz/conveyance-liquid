package com.hereliesaz.conveyance.liquid

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertTrue

class DropletShapeTest {

    private val density = Density(1f)
    private val size = Size(100f, 100f)

    private fun outlineOf(shape: DropletShape) =
        (shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Generic).path.getBounds()

    @Test
    fun `zero gravitySquash reads as a symmetric, near-circular blob`() {
        val bounds = outlineOf(DropletShape(gravitySquash = 0f))
        // Top and bottom should extend (almost) equally from center -- no flattening either way.
        val topExtent = size.height / 2f - bounds.top
        val bottomExtent = bounds.bottom - size.height / 2f
        assertTrue(
            kotlin.math.abs(topExtent - bottomExtent) < 2f,
            "top=$topExtent bottom=$bottomExtent should be nearly equal with no gravity squash",
        )
    }

    /**
     * [DropletShape.gravitySquash] only ever flattens the bottom (screen-space +y, where a
     * sessile droplet's contact patch actually sits) -- the top should reach nearly to the box
     * edge while the bottom visibly falls short of it.
     */
    @Test
    fun `gravitySquash flattens the bottom of the drop but leaves the top alone`() {
        val bounds = outlineOf(DropletShape(gravitySquash = 0.30f))
        val baseRadius = size.width / 2f
        val topExtent = size.height / 2f - bounds.top
        val bottomExtent = bounds.bottom - size.height / 2f
        assertTrue(topExtent > baseRadius * 0.9f, "top should reach nearly the full radius, was $topExtent")
        assertTrue(bottomExtent < baseRadius * 0.8f, "bottom should be visibly flattened, was $bottomExtent")
        assertTrue(bottomExtent < topExtent, "a squashed drop's bottom must not extend as far as its top")
    }

    /**
     * [DropletShape.elongation] stretches the drop along [DropletShape.dragAngleRadians] and
     * compresses it perpendicular to that -- with the drag axis aligned to the x-axis, the drop
     * should end up wider than it is tall.
     */
    @Test
    fun `elongation stretches the drop along the drag axis`() {
        val bounds = outlineOf(DropletShape(gravitySquash = 0f, elongation = 0.6f, dragAngleRadians = 0f))
        assertTrue(bounds.width > bounds.height, "width=${bounds.width} height=${bounds.height}")
    }

    @Test
    fun `elongation along a vertical drag axis stretches height instead of width`() {
        val bounds = outlineOf(
            DropletShape(gravitySquash = 0f, elongation = 0.6f, dragAngleRadians = (kotlin.math.PI / 2).toFloat()),
        )
        assertTrue(bounds.height > bounds.width, "width=${bounds.width} height=${bounds.height}")
    }

    @Test
    fun `no elongation produces a shape no wider than it is tall by more than rounding noise`() {
        val bounds = outlineOf(DropletShape(gravitySquash = 0f, elongation = 0f))
        assertTrue(kotlin.math.abs(bounds.width - bounds.height) < 2f, "width=${bounds.width} height=${bounds.height}")
    }
}
