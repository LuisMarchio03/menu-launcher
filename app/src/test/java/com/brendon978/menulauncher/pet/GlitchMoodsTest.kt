package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GlitchMoodsTest {
    @Test fun protestoEscalaEmEstagios() {
        assertEquals(ProtestStage.COMPLAINT, protestStage(1))
        assertEquals(ProtestStage.STRIKE, protestStage(2))
        assertEquals(ProtestStage.REVOLUTION, protestStage(3))
        assertEquals(ProtestStage.REVOLUTION, protestStage(9))
    }

    @Test fun contaProtestosDaUltimaHora() {
        val now = 10 * 3_600_000L
        val recent = recentProtests(listOf(now - 2 * 3_600_000L, now - 30 * 60_000L), now)
        assertEquals(1, recent.size)
    }

    @Test fun rancorDuraTresMinutosEResisteARisadinha() {
        val g = grudgeUntil(now = 1_000)
        assertEquals(1_000 + GRUDGE_MS, g)
        assertTrue(holdsGrudge(g, now = 2_000))
        assertEquals(false, holdsGrudge(g, now = g + 1))
    }

    @Test fun dramaticoSoAconteceAsVezesEComVinculoNaoAlto() {
        val low = (0 until 500).count { faints(bond = .3f, Random(it)) }
        assertTrue("às vezes", low in 20..200)
        assertEquals(0, (0 until 500).count { faints(bond = .95f, Random(it)) })
    }
}
