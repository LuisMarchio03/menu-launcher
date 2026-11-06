package com.brendon978.menulauncher.pet

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GlitchVisitTest {
    @Test fun visitaChegaESeDespede() {
        repeat(100) {
            val p = visitPlan(Random(it))
            assertEquals(VisitScene.ARRIVE, p.first())
            assertEquals(VisitScene.GOODBYE, p.last())
            assertTrue(p.size in 5..6)
            assertEquals(p.size, p.toSet().size)
        }
    }

    @Test fun todaCenaTemFalaEPose() {
        VisitScene.entries.forEach {
            val (a, b) = visitLines(it, "Bit", Random(1))
            assertTrue(a.isNotBlank() && b.isNotBlank())
            visitPoses(it, .5f, 1f, true, Hand.ROCK to Hand.PAPER)
        }
    }

    @Test fun jokenpoMostraAsMaosDepoisDaContagem() {
        val (h, g) = visitPoses(VisitScene.JOKENPO, .8f, 1f, true, Hand.ROCK to Hand.SCISSORS)
        assertEquals(Face.ROCK, h.face)
        assertEquals(Face.SCISSORS, g.face)
    }

    @Test fun visitanteEntraEVolta() {
        val fica = Offset(500f, 100f)
        val borda = Offset(1000f, 100f)
        assertEquals(borda, guestPlace(VisitScene.ARRIVE, 0f, fica, borda))
        assertEquals(fica, guestPlace(VisitScene.ARRIVE, .8f, fica, borda))
        assertEquals(fica, guestPlace(VisitScene.DANCE, .5f, fica, borda))
        assertEquals(borda, guestPlace(VisitScene.GOODBYE, 1f, fica, borda))
    }

    @Test fun bolaVaiEVolta() {
        val a = Offset(0f, 100f)
        val b = Offset(100f, 100f)
        assertEquals(a, ballAt(0f, a, b, 50f))
        assertEquals(b, ballAt(.25f, a, b, 50f))
        assertTrue(ballAt(.125f, a, b, 50f).y < 100f)
    }
}
