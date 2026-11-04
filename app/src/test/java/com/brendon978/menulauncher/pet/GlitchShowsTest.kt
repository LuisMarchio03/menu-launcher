package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GlitchShowsTest {
    @Test fun batalhaPassaPorTodasAsFases() {
        val fases = (0..100).map { battlePhase(it / 100f, evil = true) }.distinct()
        assertEquals(BattlePhase.entries.toList(), fases)
    }

    @Test fun semCloneDoMalNaoTemLuta() {
        val fases = (0..100).map { battlePhase(it / 100f, evil = false) }.toSet()
        assertFalse(BattlePhase.FINISH in fases)
        assertFalse(BattlePhase.TURN in fases)
    }

    @Test fun clonesSomemNoFim() {
        assertEquals(BattlePhase.GONE, battlePhase(1f, true))
        assertEquals(BattlePhase.GONE, battlePhase(1f, false))
    }

    @Test fun todoGolpePodeSair() {
        assertEquals(Finisher.entries.toSet(), (0 until 12).map { pickFinisher(it) }.toSet())
    }

    @Test fun nemSempreTemCloneDoMal() {
        val vezes = (0 until 1_000).count { battleHasEvil(Random(it)) }
        assertTrue(vezes in 600..800)
    }

    @Test fun showNaoRepeteECafeSoDeManha() {
        repeat(300) {
            val s = pickShow(Show.MAGIC, 20, true, Random(it))
            assertTrue(s != Show.MAGIC && s != Show.COFFEE && s !in ENTRANCES)
        }
    }

    @Test fun regaUmaVezPorDia() {
        assertFalse(canWater(100, 100))
        assertTrue(canWater(99, 100))
        repeat(300) { assertTrue(pickShow(null, 12, false, Random(it)) != Show.WATER) }
    }

    @Test fun plantaCresceEMurcha() {
        assertEquals(6, plantStage(40))
        assertTrue(plantWilted(100, 103))
        assertFalse(plantWilted(100, 102))
        assertFalse("nunca regada não está murcha", plantWilted(0, 500))
    }

    @Test fun torreDesabaAoPassarDoLimite() {
        assertEquals(1 to false, addBlock(0))
        assertEquals(TOWER_MAX to false, addBlock(TOWER_MAX - 1))
        assertEquals(0 to true, addBlock(TOWER_MAX))
    }

    @Test fun entradaSoDepoisDeUmTempoFora() {
        repeat(100) { assertNull(entranceFor(30_000, Random(it))) }
        assertNotNull((0 until 50).firstNotNullOfOrNull { entranceFor(120_000, Random(it)) })
    }

    @Test fun todoShowTemPose() {
        Show.entries.forEach { assertNotNull(it.name, showPose(ShowRun(it, 0L, 1, true), .5f, false, 1f)) }
    }

    @Test fun apagarGrafitePedeUmaEsfregada() {
        assertFalse(erased(100f, 100f))
        assertTrue(erased(151f, 100f))
    }
}
