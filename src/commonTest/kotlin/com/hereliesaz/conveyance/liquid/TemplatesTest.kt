package com.hereliesaz.conveyance.liquid

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins `liquid.drop.puddle`'s satellite shed -- specifically the defect the audit found and the
 * README documents as fixed: the shed used to run off a second, independent timer whose period
 * didn't evenly divide the wobble's own, so after the first cycle it drifted out of phase and
 * fired while the drop was nowhere near stretched. It now triggers off the wobble's own live
 * value descending through the shear level, which is exactly what [crossesShearDescending]
 * decides and what these tests drive with a simulated wobble.
 */
class TemplatesTest {

    @Test
    fun `every template id the registry advertises is present`() {
        assertEquals(
            setOf("liquid.drop.rest", "liquid.drop.drag", "liquid.drop.pair", "liquid.drop.puddle"),
            Templates.registry.keys,
        )
    }

    // --- which surfaces wobble hard enough to shed at all -------------------------------------

    @Test
    fun `a puddle wobbles past the shear level but a bead sits still`() {
        val puddle = ambientElongationPeakFor("puddle")
        val bead = ambientElongationPeakFor("bead")
        assertTrue(wobbleSheds(puddle), "a puddle should shed on its own, peak=$puddle level=$SHEAR_LEVEL")
        assertFalse(bead > SHEAR_LEVEL, "a bead should never reach the shear level, peak=$bead")
        assertFalse(wobbleSheds(bead), "a bead used with the same template must just sit still")
        // The default `drop` surface sits between the two and also stays under.
        assertFalse(wobbleSheds(ambientElongationPeakFor("drop")))
        assertFalse(wobbleSheds(ambientElongationPeakFor("anything-unrecognized")))
    }

    @Test
    fun `the puddle clears the shear level by a real but narrow margin`() {
        val puddle = ambientElongationPeakFor("puddle")
        val margin = (puddle - SHEAR_LEVEL) / SHEAR_LEVEL
        assertTrue(margin > 0f, "puddle must clear the level, margin=$margin")
        assertTrue(margin < 0.2f, "the margin is documented as narrow (~8%), was $margin")
    }

    @Test
    fun `the ambient peak never exceeds the maximum elongation the shape supports`() {
        for (surface in listOf("bead", "drop", "puddle", "unknown")) {
            assertTrue(ambientElongationPeakFor(surface) <= 1.2f, "surface=$surface")
        }
    }

    // --- the shed trigger itself --------------------------------------------------------------

    @Test
    fun `the shed is edge-triggered on the way down, not level-triggered`() {
        val below = SHEAR_LEVEL - 0.1f
        val above = SHEAR_LEVEL + 0.1f
        assertTrue(crossesShearDescending(above, below), "descending through the level sheds")
        assertFalse(crossesShearDescending(below, above), "rising through the level does not shed")
        assertFalse(crossesShearDescending(above, above), "staying stretched does not re-shed")
        assertFalse(crossesShearDescending(below, below), "staying slack does not shed")
        // Exactly at the level counts as still stretched; the shed happens on the step below it.
        assertFalse(crossesShearDescending(above, SHEAR_LEVEL))
        assertTrue(crossesShearDescending(SHEAR_LEVEL, below))
    }

    /**
     * The regression proper. Samples a puddle's actual wobble -- a reversing linear ramp between
     * 0 and its ambient peak, which is what `infiniteRepeatable(tween(..., LinearEasing),
     * RepeatMode.Reverse)` produces -- and checks the shed fires exactly once per full
     * (out-and-back) cycle, at the same phase every cycle. The old independent-timer version
     * fired on its own period instead and so drifted: by the second cycle its shed landed at a
     * different phase, and eventually at near-zero elongation.
     */
    @Test
    fun `the shed fires once per wobble cycle, at the same phase every cycle`() {
        val peak = ambientElongationPeakFor("puddle")
        val samplesPerHalf = 400
        val cycles = 6
        val shedPhases = mutableListOf<Double>()

        var previous = wobbleValue(0, samplesPerHalf, peak)
        val totalSamples = cycles * 2 * samplesPerHalf
        for (i in 1..totalSamples) {
            val value = wobbleValue(i, samplesPerHalf, peak)
            if (crossesShearDescending(previous, value)) {
                // Phase within the full out-and-back cycle, 0..1.
                shedPhases += (i % (2 * samplesPerHalf)).toDouble() / (2 * samplesPerHalf)
            }
            previous = value
        }

        assertEquals(cycles, shedPhases.size, "expected one shed per cycle, got $shedPhases")
        val first = shedPhases.first()
        for (phase in shedPhases) {
            assertTrue(
                abs(phase - first) < 0.01,
                "every shed must land at the same point in the wobble; got $shedPhases",
            )
        }
        // And that point is on the descending half, while the drop is genuinely stretched --
        // not the near-zero elongation the drifting timer eventually fired at.
        assertTrue(first > 0.5, "the shed belongs on the way back down, phase=$first")
        assertTrue(first < 0.75, "and near the top of the descent, phase=$first")
    }

    @Test
    fun `a bead's wobble never sheds no matter how long it runs`() {
        val peak = ambientElongationPeakFor("bead")
        val samplesPerHalf = 200
        var previous = wobbleValue(0, samplesPerHalf, peak)
        for (i in 1..(20 * samplesPerHalf)) {
            val value = wobbleValue(i, samplesPerHalf, peak)
            assertFalse(crossesShearDescending(previous, value), "a bead sheds nothing, ever")
            previous = value
        }
    }

    /** One sample of the reversing linear wobble: 0 -> [peak] -> 0, repeating. */
    private fun wobbleValue(sample: Int, samplesPerHalf: Int, peak: Float): Float {
        val phase = sample % (2 * samplesPerHalf)
        val rising = phase <= samplesPerHalf
        val within = if (rising) phase else 2 * samplesPerHalf - phase
        return peak * within.toFloat() / samplesPerHalf
    }

    // --- the drag template's own shear trigger ------------------------------------------------

    @Test
    fun `a drag only shears once it stretches the drop past the shear level`() {
        val shearSpeed = SHEAR_LEVEL / 0.012f // ELONGATION_SENSITIVITY
        assertFalse(dragShears(shearSpeed * 0.9f), "a gentle drag holds together")
        assertTrue(dragShears(shearSpeed * 1.1f), "a fast drag shears")
        assertFalse(dragShears(0f))
    }

    @Test
    fun `drag elongation is capped, so a violent drag can't stretch past the shape's maximum`() {
        assertEquals(1.2f, elongationForDragSpeed(100_000f), 1e-4f)
        assertTrue(dragShears(100_000f))
    }
}
