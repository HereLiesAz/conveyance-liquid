package com.hereliesaz.conveyance.liquid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidHueTest {

    @Test
    fun `of resolves every real tint name to its own tint`() {
        assertEquals(LiquidHue.mercury, LiquidHue.of("mercury"))
        assertEquals(LiquidHue.azure, LiquidHue.of("azure"))
        assertEquals(LiquidHue.verdant, LiquidHue.of("verdant"))
        assertEquals(LiquidHue.ember, LiquidHue.of("ember"))
        assertEquals(LiquidHue.violet, LiquidHue.of("violet"))
    }

    private val named = listOf(LiquidHue.mercury, LiquidHue.azure, LiquidHue.verdant, LiquidHue.ember, LiquidHue.violet)

    @Test
    fun `of hashes an unrecognized hue onto one of the five named tints, deterministically`() {
        val first = LiquidHue.of("some-unrecognized-hue")
        val second = LiquidHue.of("some-unrecognized-hue")
        assertEquals(first, second)
        assertTrue(first in named)
    }

    /**
     * The exact overflow this session's h2g2 audit found and fixed: a naive "negate if negative"
     * hash-to-index scheme can hand `%` a negative dividend when `hashCode()` is `Int.MIN_VALUE`
     * (its own negation overflows back to itself), throwing `IndexOutOfBoundsException`. [LiquidHue]
     * was written using `.mod()` from the start, so this is a regression guard, not a fix -- proven
     * here across a spread of strings rather than trying to construct one with that exact hash.
     */
    @Test
    fun `of never throws for a wide spread of unrecognized hue strings`() {
        (0..500).forEach { i ->
            val tint = LiquidHue.of("hue-$i")
            assertTrue(tint in named, "hue-$i resolved to a tint outside the five named ones")
        }
    }
}
