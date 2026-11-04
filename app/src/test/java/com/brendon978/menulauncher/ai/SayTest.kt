package com.brendon978.menulauncher.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SayTest {
    @Test fun tiraMarcacoesELinks() {
        assertEquals("Abri o YouTube.", clean("[OK] Abri o YouTube. https://youtube.com/x"))
    }

    @Test fun textaoViraCurto() {
        val t = clean("a".repeat(500))
        assertTrue(t.length <= 140)
        assertTrue(t.endsWith("..."))
    }
}
