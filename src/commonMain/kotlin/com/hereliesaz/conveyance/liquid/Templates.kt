package com.hereliesaz.conveyance.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.compose.Offer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * What a `kind: "composable"` `.azp` package's `elements[]` entry (azphalt `spec/composable.md`)
 * supplies once a host has resolved it against this library's [Templates.registry] and built the
 * live [Act] the element performs. `surface` names a size class (see [LiquidSize]); `hue` names a
 * tint (see [LiquidHue]); `scale`, unlike h2g2/expressive, doesn't set the drop's own type -- a
 * label baked into a droplet breaks the physical read this whole style is built on -- it sizes an
 * optional caption rendered beside it instead.
 */
data class ComposableRequest(
    val act: Act,
    val hue: String,
    val surface: String,
    val scale: String,
    val label: String? = null,
)

/** [ComposableRequest.surface] -> how a drop's size affects its own physics, not just its pixels. */
object LiquidSize {
    /** Smaller drops read closer to spherical -- surface tension dominates at small scale, the way a real small mercury bead does. */
    fun gravitySquashFor(surface: String): Float = when (surface) {
        "bead" -> 0.04f
        "puddle" -> 0.22f
        else -> 0.10f
    }

    fun diameterFor(surface: String): Dp = when (surface) {
        "bead" -> 32.dp
        "puddle" -> 72.dp
        else -> 48.dp
    }
}

/** The liquid composable-set's template registry -- see the `conveyance-h2g2`/`conveyance-expressive` `Templates.kt` for the pattern this follows. */
object Templates {
    val registry: Map<String, @Composable (ComposableRequest) -> Unit> = mapOf(
        "liquid.drop.rest" to { request -> RestingDrop(request) },
        "liquid.drop.drag" to { request -> DraggableDrop(request) },
        "liquid.drop.pair" to { request -> CoalescingPair(request) },
    )
}

private fun captionStyleFor(scale: String): TextStyle = when (scale) {
    "lead" -> TextStyle(fontSize = 17.sp)
    "eyebrow", "micro" -> TextStyle(fontSize = 11.sp)
    else -> TextStyle(fontSize = 14.sp)
}

/** [diameterPx] must be actual pixels (e.g. `with(LocalDensity.current) { diameter.toPx() }`) --
 *  [Brush.radialGradient]'s [center]/[radius] are local draw-space pixels, not dp. */
private fun glossBrush(tint: LiquidTint, diameterPx: Float): Brush = Brush.radialGradient(
    0f to tint.highlight,
    0.55f to tint.base,
    1f to tint.shadow,
    // Off-center toward the upper-left: a specular highlight sitting where light would actually
    // catch a curved liquid surface, not centered like a flat radial fill would be.
    center = Offset(diameterPx * 0.32f, diameterPx * 0.28f),
    radius = diameterPx * 0.85f,
)

/** A drop at rest: [DropletShape] with only [LiquidSize.gravitySquashFor]'s static flattening, no drag stretch. */
@Composable
fun RestingDrop(request: ComposableRequest) {
    val tint = LiquidHue.of(request.hue)
    val diameter = LiquidSize.diameterFor(request.surface)
    val diameterPx = with(LocalDensity.current) { diameter.toPx() }
    val squash = LiquidSize.gravitySquashFor(request.surface)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Offer(act = request.act) {
            Box(
                modifier = Modifier
                    .size(diameter)
                    .clip(DropletShape(gravitySquash = squash))
                    .background(glossBrush(tint, diameterPx)),
            )
        }
        request.label?.let {
            BasicText(text = it, modifier = Modifier.padding(top = 4.dp), style = captionStyleFor(request.scale))
        }
    }
}

private const val ELONGATION_SENSITIVITY = 0.012f
private const val MAX_ELONGATION = 1.2f
private val RELEASE_SPRING = spring<Float>(dampingRatio = 0.35f, stiffness = 180f)

/** Fission point: past this fraction of [MAX_ELONGATION], the neck between the stretched body and its lagging tail shears off a satellite droplet. */
private const val SHEAR_THRESHOLD = 0.85f
private const val SATELLITE_DRIFT_MS = 500

/**
 * A drop that stretches along the direction it's dragged and, on release, overshoots and wobbles
 * back to rest -- an underdamped spring (`dampingRatio = 0.35`) back to zero elongation, which is
 * a real damped-harmonic-oscillator model, the same physics a real droplet's surface tension
 * enacts when it's disturbed and settles, not a canned "bounce" easing curve.
 *
 * Dragged fast enough (past [SHEAR_THRESHOLD] of [MAX_ELONGATION]), the stretched neck between
 * the drop's leading body and its lagging tail shears: a small satellite droplet detaches at the
 * trailing tip and drifts away, fading, over [SATELLITE_DRIFT_MS] -- real fission, the way a
 * dragged mercury bead actually sheds a smaller bead when pulled too fast for surface tension to
 * hold it together, not an ornamental particle effect.
 */
@Composable
fun DraggableDrop(request: ComposableRequest) {
    val tint = LiquidHue.of(request.hue)
    val diameter = LiquidSize.diameterFor(request.surface)
    val diameterPx = with(LocalDensity.current) { diameter.toPx() }
    val squash = LiquidSize.gravitySquashFor(request.surface)
    val scope = rememberCoroutineScope()
    val elongation = remember { Animatable(0f) }
    var dragAngle by remember { mutableFloatStateOf(0f) }
    val satellite = remember { Animatable(1f) }
    var shearing by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Offer(act = request.act) {
            Box(modifier = Modifier.size(diameter * 1.6f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(diameter)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { _, dragAmount ->
                                    dragAngle = atan2(dragAmount.y, dragAmount.x)
                                    val speed = hypot(dragAmount.x, dragAmount.y)
                                    val target = (speed * ELONGATION_SENSITIVITY).coerceAtMost(MAX_ELONGATION)
                                    scope.launch { elongation.snapTo(target) }
                                    if (target >= SHEAR_THRESHOLD * MAX_ELONGATION && !shearing) {
                                        shearing = true
                                        scope.launch {
                                            satellite.snapTo(0f)
                                            satellite.animateTo(1f, tween(SATELLITE_DRIFT_MS))
                                            shearing = false
                                        }
                                    }
                                },
                                onDragEnd = { scope.launch { elongation.animateTo(0f, RELEASE_SPRING) } },
                                onDragCancel = { scope.launch { elongation.animateTo(0f, RELEASE_SPRING) } },
                            )
                        }
                        .clip(
                            DropletShape(
                                gravitySquash = squash,
                                elongation = elongation.value,
                                dragAngleRadians = dragAngle,
                            ),
                        )
                        .background(glossBrush(tint, diameterPx)),
                )
                if (satellite.value < 1f) {
                    val satelliteDiameter = diameter * 0.32f
                    // Drifts outward past the main drop's trailing edge (opposite the drag
                    // direction) as it shears free, shrinking and fading as it goes.
                    val driftPx = diameterPx * 0.7f * satellite.value
                    Box(
                        modifier = Modifier
                            .size(satelliteDiameter)
                            .graphicsLayer {
                                alpha = 1f - satellite.value
                                scaleX = 1f - satellite.value * 0.4f
                                scaleY = scaleX
                            }
                            .offset {
                                IntOffset(
                                    x = (-driftPx * cos(dragAngle.toDouble())).toInt(),
                                    y = (-driftPx * sin(dragAngle.toDouble())).toInt(),
                                )
                            }
                            .clip(DropletShape(gravitySquash = squash))
                            .background(glossBrush(tint, diameterPx * 0.32f)),
                    )
                }
            }
        }
        request.label?.let {
            BasicText(text = it, modifier = Modifier.padding(top = 4.dp), style = captionStyleFor(request.scale))
        }
    }
}

/**
 * Two drops coalescing into one, driven by the act's own state the same way
 * `conveyance-bacterium`'s `bacterium.cell.divide` drives its separation -- [ActState.Yielding]'s
 * live progress closes the gap, [ActState.Settled] means fully merged. Real geometry (two circles
 * plus a growing connecting neck, `LiquidPairShape`), not a crossfade between two images.
 */
@Composable
fun CoalescingPair(request: ComposableRequest) {
    val tint = LiquidHue.of(request.hue)
    val diameter = LiquidSize.diameterFor(request.surface)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Offer(act = request.act) {
            val proximity = when (state) {
                is ActState.Settled -> 1f
                is ActState.Yielding -> yielding ?: 0f
                else -> 0f
            }
            val diameterPx = with(LocalDensity.current) { (diameter * 1.6f).toPx() }
            Box(
                modifier = Modifier
                    .size(diameter * 1.6f)
                    .clip(LiquidPairShape(proximity = proximity))
                    .background(glossBrush(tint, diameterPx)),
            )
        }
        request.label?.let {
            BasicText(text = it, modifier = Modifier.padding(top = 4.dp), style = captionStyleFor(request.scale))
        }
    }
}
