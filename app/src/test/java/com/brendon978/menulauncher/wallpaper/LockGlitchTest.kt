package com.brendon978.menulauncher.wallpaper

import com.brendon978.menulauncher.pet.Face
import com.brendon978.menulauncher.pet.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LockGlitchTest {
    @Test fun naoRepeteAMesmaCoisaEmSeguida() {
        repeat(200) { assertNotEquals(LockAct.WAVE, pickAct(LockAct.WAVE, Random(it))) }
    }

    @Test fun nemSempreFazAlgoQuandoATelaAcende() {
        val r = (0 until 1_000).map { wakeActDelay(Random(it)) }
        val faz = r.count { it != null }
        assertTrue("fez em $faz de 1000", faz in 300..500)
        assertTrue(r.filterNotNull().all { it in 1_500L until 4_500L })
    }

    @Test fun depoisDeUmaCoisaDemoraBastante() {
        repeat(200) { assertTrue(nextActGap(Random(it)) >= 20_000L) }
    }

    @Test fun dormindoOuCansadoNaoApronta() {
        assertFalse(actsIn(Mood.SLEEPING))
        assertFalse(actsIn(Mood.TIRED))
        assertFalse(actsIn(Mood.DANCING))
        assertTrue(actsIn(Mood.NORMAL))
    }

    @Test fun paradoRedesenhaPoucoEAgindoRedesenhaMais() {
        assertTrue(lockFrameDelay(null, Mood.NORMAL) >= 200L)
        assertTrue(lockFrameDelay(null, Mood.SLEEPING) >= 200L)
        assertTrue(lockFrameDelay(LockAct.HOP, Mood.NORMAL) <= 50L)
    }

    @Test fun humorVenceAAcao() {
        assertEquals(Face.SLEEP, lockPose(LockAct.WAVE, .5f, Mood.SLEEPING, 1f).face)
        assertEquals(Face.TIRED, lockPose(null, 0f, Mood.TIRED, 1f).face)
        assertEquals(Face.MUSIC, lockPose(null, 0f, Mood.DANCING, 1f).face)
    }

    @Test fun cadaAcaoMudaAPose() {
        val parado = lockPose(null, 0f, Mood.NORMAL, 1f)
        LockAct.entries.forEach { assertNotEquals(it.name, parado, lockPose(it, .25f, Mood.NORMAL, 1f)) }
    }

    @Test fun espreguicarAbreOsBracosParaFora() {
        val p = lockPose(LockAct.STRETCH, .25f, Mood.NORMAL, 1f)
        assertTrue(p.armL > 90f)
        assertTrue(p.armR < -90f)
    }
}
