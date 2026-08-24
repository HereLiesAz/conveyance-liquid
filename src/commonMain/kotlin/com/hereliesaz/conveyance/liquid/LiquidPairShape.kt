package com.hereliesaz.conveyance.liquid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private const val NECK_VISIBLE_FROM = 0.4f

/**
 * Coalescence's outline: two circular drops whose centers pull together as [proximity] goes 0→1,
 * bridged by a growing rectangular neck once they're close enough to touch -- surface tension
 * pulling the two together and thickening the bridge between them, not two shapes crossfading
 * into one. At `proximity = 0` the drops sit apart with no neck; at `proximity = 1` their centers
 * coincide and the neck has grown to fill the gap, reading as one merged drop.
 *
 * The geometry mirrors `conveyance-bacterium`'s `MitosisShape` (two lobes plus a connecting belt)
 * run in the opposite narrative direction: pulling together under surface tension there, instead
 * of pinching apart under a cleavage furrow.
 */
class LiquidPairShape(private val proximity: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val separateRadius = minOf(size.width, size.height) / 2f * 0.6f
        val mergedRadius = minOf(size.width, size.height) / 2f
        val maxCenterOffset = size.width * 0.30f

        val t = proximity.coerceIn(0f, 1f)
        val offset = maxCenterOffset * (1f - t)
        val radius = separateRadius + (mergedRadius - separateRadius) * t
        val c1 = Offset(cx - offset, cy)
        val c2 = Offset(cx + offset, cy)

        val path = Path().apply {
            addOval(Rect(center = c1, radius = radius))
            addOval(Rect(center = c2, radius = radius))
        }

        if (t > NECK_VISIBLE_FROM) {
            val neckGrowth = (t - NECK_VISIBLE_FROM) / (1f - NECK_VISIBLE_FROM)
            val neckHalfHeight = radius * neckGrowth * 0.85f
            if (neckHalfHeight > 0.5f) {
                path.addPath(
                    Path().apply {
                        moveTo(c1.x, cy - neckHalfHeight)
                        lineTo(c2.x, cy - neckHalfHeight)
                        lineTo(c2.x, cy + neckHalfHeight)
                        lineTo(c1.x, cy + neckHalfHeight)
                        close()
                    },
                )
            }
        }

        return Outline.Generic(path)
    }
}
