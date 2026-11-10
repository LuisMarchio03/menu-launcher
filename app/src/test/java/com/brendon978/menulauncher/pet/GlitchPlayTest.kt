package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GlitchPlayTest {
    private val world = FlyWorld(Area(0f, 0f, 1000f, 2000f), 150f, 100f, 200f)

    @Test fun peteleco() {
        assertTrue(isFlick(200f, 150, 150f))
        assertFalse("devagar é carinho", isFlick(200f, 600, 150f))
        assertFalse("curto é toque", isFlick(60f, 150, 150f))
    }

    @Test fun cocegas() {
        assertTrue(isTickle(6, 1_500))
        assertFalse(isTickle(3, 900))
        assertFalse(isTickle(6, 3_000))
    }

    @Test fun memoria() {
        val s = listOf(2, 0, 3)
        assertEquals(MemoryTap.CORRECT, memoryTap(s, 0, 2))
        assertEquals(MemoryTap.ROUND_DONE, memoryTap(s, 2, 3))
        assertEquals(MemoryTap.WRONG, memoryTap(s, 1, 3))
        assertTrue(memoryFlashMs(1) > memoryFlashMs(10))
        assertEquals(230L, memoryFlashMs(40))
    }

    @Test fun vooCaiSemToqueESobeComToque() {
        val s = FlyState(800f)
        val caindo = flyStep(s, .1f, world, Random(1))
        assertTrue(caindo.y > s.y)
        val subindo = flyStep(flyTap(s, world), .05f, world, Random(1))
        assertTrue(subindo.y < s.y)
    }

    @Test fun vooBateNoChao() {
        var s = FlyState(1700f)
        repeat(60) { s = flyStep(s, .03f, world, Random(1)) }
        assertTrue(s.crashed)
    }

    @Test fun vooMarcaPontoAoPassarOMuro() {
        val r = flyStep(FlyState(900f, walls = listOf(FlyWall(-5f, 1000f))), .01f, world, Random(1))
        assertEquals(1, r.score)
        assertFalse(r.crashed)
    }

    @Test fun vooBateNoMuro() {
        val r = flyStep(FlyState(100f, walls = listOf(FlyWall(160f, 1500f))), .01f, world, Random(1))
        assertTrue(r.crashed)
    }

    @Test fun faltaDeTempoParaSpawnarNaoQuebra() {
        val r = flyStep(FlyState(900f), .016f, world, Random(1))
        assertEquals(1, r.walls.size)
    }

    @Test fun falaConformeOVinculo() {
        repeat(50) {
            val l = idleLine(15, .1f, Random(it))
            assertTrue(l in listOf("Hmpf.", "Tô de olho.", "Lembro de cada cutucada.", "Nem vem."))
        }
    }

    @Test fun falaSozinhoComCalma() {
        repeat(100) { assertTrue(nextIdleGapMs(Random(it)) >= 120_000L) }
    }

    @Test fun vooEsperaOPrimeiroToque() {
        val s = FlyState(800f, started = false)
        assertEquals(800f, flyStep(s, 1f, world, Random(1)).y)
        assertTrue(flyTap(s, world).started)
    }

    @Test fun tetoNaoDerruba() {
        val r = flyStep(FlyState(1f, vy = -2_000f), .05f, world, Random(1))
        assertFalse(r.crashed)
        assertEquals(0f, r.y)
    }
}
