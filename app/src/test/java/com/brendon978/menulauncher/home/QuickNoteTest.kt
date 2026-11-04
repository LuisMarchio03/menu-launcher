package com.brendon978.menulauncher.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickNoteTest {
    @Test fun tiraAsSobras() {
        assertEquals("comprar pão", cleanNote("  comprar pão \n"))
        assertEquals("", cleanNote("   "))
    }

    @Test fun cortaNoLimite() {
        assertEquals(NOTE_MAX, cleanNote("a".repeat(NOTE_MAX + 50)).length)
    }

    @Test fun naoParteEmojiNoMeio() {
        val nota = cleanNote("a".repeat(NOTE_MAX - 1) + "😀")
        assertEquals("a".repeat(NOTE_MAX - 1), nota)
        assertTrue(nota.none { it.isSurrogate() })
    }
}
