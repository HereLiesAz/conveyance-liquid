package com.hereliesaz.conveyance.liquid

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidSizeTest {

    @Test
    fun `gravitySquashFor is smallest for a bead and largest for a puddle`() {
        val bead = LiquidSize.gravitySquashFor("bead")
        val default = LiquidSize.gravitySquashFor("anything-else")
        val puddle = LiquidSize.gravitySquashFor("puddle")
        assertTrue(bead < default, "a bead should read closer to spherical than the default")
        assertTrue(default < puddle, "a puddle should read more flattened than the default")
    }

    @Test
    fun `diameterFor is smallest for a bead and largest for a puddle`() {
        assertEquals(32.dp, LiquidSize.diameterFor("bead"))
        assertEquals(72.dp, LiquidSize.diameterFor("puddle"))
        assertEquals(48.dp, LiquidSize.diameterFor("anything-else"))
        assertTrue(LiquidSize.diameterFor("bead") < LiquidSize.diameterFor("anything-else"))
        assertTrue(LiquidSize.diameterFor("anything-else") < LiquidSize.diameterFor("puddle"))
    }
}
