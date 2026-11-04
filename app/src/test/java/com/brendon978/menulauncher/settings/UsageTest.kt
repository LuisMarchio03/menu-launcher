package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageTest {
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val c = AppKey("c", "C", 0)

    @Test fun abrirSomaEOsOutrosEsfriam() {
        val u = Usage.bump(mapOf(a to 2f), b)
        assertEquals(1f, u.getValue(b), .001f)
        assertTrue(u.getValue(a) < 2f)
    }

    @Test fun maisUsadosSemOsFavoritos() {
        val u = mapOf(a to 5f, b to 3f, c to 1f)
        assertEquals(listOf(b, c), Usage.top(u, n = 4, exclude = setOf(a)))
        assertEquals(listOf(a), Usage.top(u, n = 1, exclude = emptySet()))
    }

    @Test fun quaseZeradoSomeDaConta() {
        var u = mapOf(a to .02f)
        u = Usage.bump(u, b)
        assertEquals(setOf(b), u.keys)
    }
}
