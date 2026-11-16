package net.nexus.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DropAndStarsTest {
    @Test fun tapPulseSqueezesThenPopsTwoPercentAndSettles() {
        assertEquals(1f, dropPulse(0f), 1e-4f)
        assertEquals(0.90f, dropPulse(0.09f), 1e-3f)
        assertEquals(1.02f, dropPulse(0.26f), 1e-3f)
        assertEquals(1f, dropPulse(0.6f), 1e-4f)
        assertTrue((0..60).all { dropPulse(it / 100f) in 0.899f..1.021f })
    }

    @Test fun shootingStarEveryTenSeconds() {
        assertEquals(-1f, shootingProgress(1f), 0f)
        assertTrue(shootingProgress(3.3f) in 0f..1f)
        assertEquals(-1f, shootingProgress(8f), 0f)
        assertTrue(shootingProgress(13.3f) in 0f..1f)
    }
}
