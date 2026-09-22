package com.hereliesaz.conveyance.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

/**
 * Real two-body coalescence: [first] and [second] are two **independently addressed**
 * [RestingDrop]s, each free to end up wherever [firstPlacement]/[secondPlacement] put them (an
 * `.align(...)`, a `.offset(...)`, a drag gesture -- whatever the host's own layout is already
 * doing) -- not [LiquidPairShape]/`liquid.drop.pair`'s single shared-center element pretending to
 * be two. This measures each drop's actual on-screen center via `onGloballyPositioned` and, only
 * when they happen to be close enough, paints a connecting neck between them -- a decorative
 * overlay reading two already-placed elements' real positions, never repositioning either one
 * itself. Neither drop's own act is touched.
 */
@Composable
fun LiquidField(
    first: ComposableRequest,
    second: ComposableRequest,
    modifier: Modifier = Modifier,
    firstPlacement: Modifier = Modifier,
    secondPlacement: Modifier = Modifier,
) {
    var firstCenter by remember { mutableStateOf<Offset?>(null) }
    var secondCenter by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val firstRadiusPx = with(density) { (LiquidSize.diameterFor(first.surface) / 2).toPx() }
    val secondRadiusPx = with(density) { (LiquidSize.diameterFor(second.surface) / 2).toPx() }
    val firstTint = LiquidHue.of(first.hue)
    val secondTint = LiquidHue.of(second.hue)

    Box(modifier = modifier) {
        val a = firstCenter
        val b = secondCenter
        if (a != null && b != null) {
            val closeness = liquidNeckCloseness((a - b).getDistance(), firstRadiusPx, secondRadiusPx)
            if (closeness != null) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawLiquidNeck(a, b, closeness, minOf(firstRadiusPx, secondRadiusPx), firstTint, secondTint)
                }
            }
        }
        Box(modifier = firstPlacement.onGloballyPositioned { firstCenter = it.boundsInParent().center }) {
            RestingDrop(first)
        }
        Box(modifier = secondPlacement.onGloballyPositioned { secondCenter = it.boundsInParent().center }) {
            RestingDrop(second)
        }
    }
}

private const val NECK_MAX_HALF_HEIGHT_FRACTION = 0.7f

/** How far past actually touching two drops can still be and have a neck painted between them. */
internal const val NECK_THRESHOLD_FACTOR = 1.6f

/**
 * How fully formed the neck between two drops whose centers are [distance] apart is, or `null`
 * when they are too far apart for one to exist at all. Necks form once the drops' edges are
 * within their own combined radii of touching -- real proximity relative to how big either drop
 * actually is, not an arbitrary flat pixel threshold.
 *
 * Pure arithmetic, deliberately separate from [drawLiquidNeck]'s `DrawScope` call so it can be
 * tested without a Compose UI harness.
 */
internal fun liquidNeckCloseness(distance: Float, firstRadiusPx: Float, secondRadiusPx: Float): Float? {
    val touchDistance = firstRadiusPx + secondRadiusPx
    val neckThreshold = touchDistance * NECK_THRESHOLD_FACTOR
    if (distance >= neckThreshold) return null
    return (1f - (distance - touchDistance).coerceAtLeast(0f) / (neckThreshold - touchDistance))
        .coerceIn(0f, 1f)
}

/**
 * The neck's gradient stops: **each drop's own tint at its own end**, darkening toward the middle
 * where a real liquid neck reads thinnest and dimmest. The midpoint is the two drops' shadows
 * mixed evenly, so neither end's color wins the middle outright.
 *
 * This is the whole of the defect the audit found: the neck used to paint [tintA]'s highlight at
 * *both* ends and [tintA]'s shadow in the middle, so a neck between two differently-hued drops
 * showed nothing of the second drop's hue at all. Extracted out of [drawLiquidNeck] so exactly
 * that can be asserted without rendering anything.
 */
internal fun liquidNeckGradientStops(tintA: LiquidTint, tintB: LiquidTint): List<Pair<Float, Color>> = listOf(
    0f to tintA.highlight,
    0.5f to lerp(tintA.shadow, tintB.shadow, 0.5f),
    1f to tintB.highlight,
)

private fun DrawScope.drawLiquidNeck(
    a: Offset,
    b: Offset,
    closeness: Float,
    radiusPx: Float,
    tintA: LiquidTint,
    tintB: LiquidTint,
) {
    val delta = b - a
    val distance = delta.getDistance()
    if (distance <= 0f) return
    val direction = Offset(delta.x / distance, delta.y / distance)
    val perpendicular = Offset(-direction.y, direction.x)
    val neckHalfHeight = radiusPx * NECK_MAX_HALF_HEIGHT_FRACTION * closeness

    val path = Path().apply {
        moveTo(a.x + perpendicular.x * neckHalfHeight, a.y + perpendicular.y * neckHalfHeight)
        lineTo(b.x + perpendicular.x * neckHalfHeight, b.y + perpendicular.y * neckHalfHeight)
        lineTo(b.x - perpendicular.x * neckHalfHeight, b.y - perpendicular.y * neckHalfHeight)
        lineTo(a.x - perpendicular.x * neckHalfHeight, a.y - perpendicular.y * neckHalfHeight)
        close()
    }
    // Highlight at each end -- catching the light the same way each drop's own gloss does, its
    // own tint at its own end, so a neck between two differently-hued drops actually reads as
    // each drop's own color at that end rather than one drop's tint painted onto both --
    // darkening toward the middle, where a real liquid neck reads thinnest and dimmest, not a
    // flat fill. See [liquidNeckGradientStops], where that mapping actually lives.
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colorStops = liquidNeckGradientStops(tintA, tintB).toTypedArray(),
            start = a,
            end = b,
        ),
        alpha = closeness,
    )
}
