package com.brendon978.menulauncher.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GesturesTest {
    @Test fun arrastoCurtoNaoConta() {
        assertNull(swipeOf(totalDy = 40f, thresholdPx = 100f))
        assertNull(swipeOf(totalDy = -99f, thresholdPx = 100f))
    }

    @Test fun direcaoPeloSinal() {
        assertEquals(Swipe.DOWN, swipeOf(totalDy = 150f, thresholdPx = 100f))
        assertEquals(Swipe.UP, swipeOf(totalDy = -150f, thresholdPx = 100f))
    }
}
