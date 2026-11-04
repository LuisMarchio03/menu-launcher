package com.brendon978.menulauncher.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeActionsTest {
    @Test fun valeParaOsDoisLados() {
        assertTrue(swipeOpensActions(200f, 200f))
        assertTrue(swipeOpensActions(-200f, 200f))
    }

    @Test fun arrastoCurtoOuDesistenciaNaoAbre() {
        assertFalse(swipeOpensActions(199f, 200f))
        assertFalse(swipeOpensActions(0f, 200f))
    }
}
