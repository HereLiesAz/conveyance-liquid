package com.hereliesaz.conveyance.liquid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val BLOB_POINTS = 28

/**
 * A liquid drop's outline, drawn from actual droplet physics rather than a fixed corner-radius
 * shape:
 *
 * - **[gravitySquash]** flattens the bottom (screen-space +y) of the drop -- a sessile droplet
 *   rests on a surface with its contact patch flattened by gravity while surface tension keeps
 *   the rest of it round. A tiny bead (small [gravitySquash]) reads as nearly spherical, the way
 *   a real small mercury bead does; a larger puddle reads visibly flattened, the way capillary
 *   length actually works.
 * - **[elongation]** / **[dragAngleRadians]** stretch the drop along the direction of motion and
 *   compress it perpendicular to that -- momentum dragging the leading/trailing edge out while
 *   surface tension resists the rest, an approximation of a droplet's response to being moved
 *   across a surface rather than a canned "squash and stretch" curve.
 *
 * The outline itself is sampled at [BLOB_POINTS] angles around the center and smoothed through a
 * closed quadratic path (each original point as a curve control, its neighbor's midpoint as the
 * anchor) -- a standard way to turn a radius(θ) function into a smooth blob rather than a
 * faceted polygon.
 */
class DropletShape(
    private val gravitySquash: Float = 0.10f,
    private val elongation: Float = 0f,
    private val dragAngleRadians: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = minOf(size.width, size.height) / 2f

        val points = (0 until BLOB_POINTS).map { i ->
            val theta = 2.0 * PI * i / BLOB_POINTS
            // sin(theta) is 0 at top/bottom-of-circle poles' sides and peaks at the bottom in a
            // y-down coordinate system (theta = PI/2) -- exactly where a resting drop flattens.
            val bottomWeight = max(0.0, sin(theta)).toFloat()
            val gravityFactor = 1f - gravitySquash * bottomWeight

            // Peaks (=1) aligned with dragAngleRadians, troughs (~0) perpendicular to it --
            // stretches the leading/trailing edge, compresses the sides. This does not preserve
            // area (that would need axisAlignment's mean subtracted, 0.5 not 0.33): the shape
            // visibly balloons at high elongation, which reads fine for a droplet mid-stretch --
            // stretched liquid catching more light is a real visual cue -- but isn't an area-
            // conserving deformation.
            val axisAlignment = cos(theta - dragAngleRadians).toFloat().let { it * it }
            val stretchFactor = (1f + elongation * (axisAlignment - 0.33f)).coerceAtLeast(0.4f)

            val r = baseRadius * gravityFactor * stretchFactor
            Offset(
                x = cx + (r * cos(theta)).toFloat(),
                y = cy + (r * sin(theta)).toFloat(),
            )
        }

        return Outline.Generic(smoothClosedPath(points))
    }
}

private fun smoothClosedPath(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    val midpoints = (0 until n).map { i -> lerp(points[i], points[(i + 1) % n], 0.5f) }
    path.moveTo(midpoints[n - 1].x, midpoints[n - 1].y)
    for (i in 0 until n) {
        path.quadraticTo(points[i].x, points[i].y, midpoints[i].x, midpoints[i].y)
    }
    path.close()
    return path
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
