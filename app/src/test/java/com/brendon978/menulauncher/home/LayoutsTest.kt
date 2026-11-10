package com.brendon978.menulauncher.home

import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class LayoutsTest {
    private fun app(n: String) = AppEntry(AppKey("p.$n", "p.$n.M", 0), n)

    @Test fun lequeFicaNoQuartoDoCantoDireito() {
        val slots = fanSlots(5, 300f, rightSide = true)
        assertEquals(5, slots.size)
        // todos para cima e para a esquerda do canto, na mesma distância
        assertTrue(slots.all { it.first <= 0.01f && it.second <= 0.01f })
        assertTrue(slots.all { abs(hypot(it.first, it.second) - 300f) < 0.5f })
    }

    @Test fun lequeDoLadoEsquerdoEhOEspelho() {
        val r = fanSlots(4, 200f, rightSide = true)
        val l = fanSlots(4, 200f, rightSide = false)
        assertEquals(r.map { -it.first }, l.map { it.first })
    }

    @Test fun lequeComMuitosAbreUmSegundoArco() {
        val slots = fanSlots(9, 300f, rightSide = true)
        assertEquals(9, slots.size)
        assertTrue(slots.drop(6).all { hypot(it.first, it.second) > 300f })
    }

    @Test fun paginasSaoFavoritosEDepoisAsPastas() {
        val a = app("A"); val b = app("B"); val c = app("C")
        val byKey = listOf(a, b, c).associateBy { it.key }
        val pages = favoritePages(listOf(a), mapOf("Zeta" to listOf(c.key), "Alfa" to listOf(b.key), "Vazia" to emptyList()), byKey)
        assertEquals(listOf("Favoritos", "Alfa", "Zeta"), pages.map { it.first })
        assertEquals(listOf(b), pages[1].second)
    }
}
