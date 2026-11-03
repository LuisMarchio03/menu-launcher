package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPhysicsTest {
    // ---- carinho: dedo indo e voltando ----
    @Test fun idaEVoltaContamComoCarinho() {
        val xs = listOf(0f, 20f, 40f, 20f, 0f, 20f, 40f)
        assertEquals(2, countReversals(xs, minTravel = 15f))
    }

    @Test fun tremidinhaNaoConta() {
        assertEquals(0, countReversals(listOf(0f, 3f, 1f, 4f, 2f, 5f), minTravel = 15f))
    }

    @Test fun arrastarRetoNaoECarinho() {
        assertEquals(0, countReversals((0..20).map { it * 10f }, minTravel = 15f))
    }

    // ---- arremesso: gravidade, quique e pouso ----
    private val area = Area(0f, 0f, 400f, 800f)
    private val w = 40f
    private val h = 60f

    private fun fall(start: Body, supports: List<Area> = emptyList(), maxSteps: Int = 2_000): Pair<Body, Int> {
        var b = start
        repeat(maxSteps) { i ->
            val (next, settled) = physicsStep(b, 1f / 60f, area, supports, w, h)
            b = next
            if (settled) return b to i
        }
        return b to maxSteps
    }

    @Test fun caiNoChaoEPara() {
        val (b, steps) = fall(Body(100f, 100f, 0f, 0f))
        assertEquals(area.b - h, b.y, 0.5f)
        assertTrue("assenta em tempo razoável", steps < 600)
    }

    @Test fun pousaEmCimaDaLista() {
        val list = Area(0f, 500f, 400f, 800f)
        val (b, _) = fall(Body(100f, 100f, 0f, 0f), supports = listOf(list))
        assertEquals(500f - h, b.y, 0.5f)
    }

    @Test fun naoAtravessaParedes() {
        var b = Body(100f, 100f, 3_000f, -1_500f)
        repeat(600) {
            b = physicsStep(b, 1f / 60f, area, emptyList(), w, h).first
            assertTrue(b.x >= area.l - .5f && b.x + w <= area.r + .5f)
            assertTrue(b.y >= area.t - .5f && b.y + h <= area.b + .5f)
        }
    }

    @Test fun quicaPerdendoForca() {
        var b = Body(100f, 100f, 0f, 0f)
        val bounces = mutableListOf<Float>()
        repeat(1_000) {
            val prevVy = b.vy
            b = physicsStep(b, 1f / 60f, area, emptyList(), w, h).first
            if (prevVy > 0f && b.vy < 0f) bounces += -b.vy
        }
        assertTrue("quicou", bounces.isNotEmpty())
        assertTrue("cada quique mais fraco", bounces.zipWithNext().all { (a, c) -> c < a })
    }

    @Test fun subindoNaoPousaNoQueEstaEmbaixo() {
        // jogado para cima saindo de dentro da faixa de um apoio: não gruda nele
        val ledge = Area(0f, 300f, 400f, 320f)
        val (b, _) = physicsStep(Body(100f, 260f, 0f, -800f), 1f / 60f, area, listOf(ledge), w, h)
        assertTrue(b.vy < 0f)
    }
}
