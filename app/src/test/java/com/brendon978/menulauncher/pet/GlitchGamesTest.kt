package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlitchGamesTest {
    private val area = Area(0f, 0f, 100f, 100f)

    @Test fun bugRebateNaBorda() {
        val b = moveBug(Bug(1, 99f, 50f, 100f, 0f), .1f, area)
        assertEquals(100f, b.x)
        assertEquals(-100f, b.vx)
    }

    @Test fun bugAndaEmLinhaReta() {
        val b = moveBug(Bug(1, 10f, 10f, 10f, 20f), 1f, area)
        assertEquals(20f, b.x)
        assertEquals(30f, b.y)
    }

    @Test fun placarDaCaca() {
        assertTrue(bugScoreLine(5, 3).contains("venceu"))
        assertTrue(bugScoreLine(3, 5).contains("Exterminador"))
        assertTrue(bugScoreLine(4, 4).contains("Empate"))
    }

    @Test fun duelo() {
        assertEquals(DuelRound.YOU, duelRound(200, 300, false))
        assertEquals(DuelRound.HIM, duelRound(400, 300, false))
        assertEquals("sem toque, ponto dele", DuelRound.HIM, duelRound(null, 300, false))
        assertEquals("queimou a largada", DuelRound.HIM, duelRound(100, 300, true))
    }

    @Test fun recorde() {
        assertEquals(250L, bestTime(0L, 250))
        assertEquals(200L, bestTime(250L, 200))
        assertEquals(200L, bestTime(200L, 300))
    }

    @Test fun barriga() {
        assertEquals(1f, bellyAfterMeal(.9f))
        assertEquals(.5f, bellyAfterTime(.6f, 18f), .001f)
        assertEquals(0f, bellyAfterTime(.1f, 60f))
    }

    @Test fun menuSempreDentroDaTela() {
        val tela = Area(0f, 0f, 400f, 1_000f)
        assertEquals("cabe acima", 192f, menuTop(700f, 100f, 500f, tela))
        assertEquals("não cabe acima, vai para baixo", 408f, menuTop(300f, 100f, 300f, tela))
        assertEquals("não cabe em nenhum dos dois: encosta no fim da tela", 400f, menuTop(450f, 100f, 600f, tela))
        assertEquals("maior que a tela: começa no topo", 0f, menuTop(500f, 100f, 1_200f, tela))
    }

    @Test fun poseDeFotoNaoRepeteEmSeguida() {
        repeat(200) { assertTrue(pickPhotoPose(PhotoPose.DAB, kotlin.random.Random(it)) != PhotoPose.DAB) }
    }

    @Test fun cadaPoseDeFotoEDiferente() {
        val poses = PhotoPose.entries.map { photoPose(it, false, 1f) }
        assertEquals(poses.size, poses.toSet().size)
    }

    @Test fun andarEmagreceMaisRapido() {
        assertEquals(.9f, bellyAfterTime(1f, 18f), .001f)
        assertEquals(.7f, bellyAfterTime(1f, 18f, walking = true), .001f)
    }

    @Test fun glitchAcompanhaSeuRecorde() {
        assertEquals(290L..480L, duelGlitchRange(0L))
        assertEquals(171L..251L, duelGlitchRange(181L))
        assertEquals("nunca fica impossível", 150L, duelGlitchRange(120L).first)
        assertEquals("nem lento demais", 480L, duelGlitchRange(450L).last)
    }
}
