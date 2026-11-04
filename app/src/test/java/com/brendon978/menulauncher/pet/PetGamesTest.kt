package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetGamesTest {
    // ---- jokenpô ----
    @Test fun jokenpoRegras() {
        assertEquals(Outcome.WIN, jokenpo(Hand.ROCK, Hand.SCISSORS))
        assertEquals(Outcome.WIN, jokenpo(Hand.PAPER, Hand.ROCK))
        assertEquals(Outcome.WIN, jokenpo(Hand.SCISSORS, Hand.PAPER))
        assertEquals(Outcome.LOSE, jokenpo(Hand.ROCK, Hand.PAPER))
        assertEquals(Outcome.DRAW, jokenpo(Hand.PAPER, Hand.PAPER))
    }

    @Test fun glitchJogaDeTudo() {
        val r = Random(4)
        assertEquals(Hand.entries.toSet(), (0 until 100).map { glitchHand(r) }.toSet())
    }

    // ---- esconde-esconde ----
    @Test fun achouQuandoTocaNoEsconderijo() {
        val hide = Area(100f, 100f, 300f, 200f)
        assertEquals(HideGuess.FOUND, guessHide(hide, 150f, 150f))
        assertEquals(HideGuess.WARM, guessHide(hide, 150f, 330f))
        assertEquals(HideGuess.COLD, guessHide(hide, 900f, 1800f))
    }

    // ---- aprender a se encolher (condicionamento) ----
    @Test fun muitaCutucadaEnsinaAEncolher() {
        var e = 0f
        repeat(8) { e = expectPoke(e, poked = true) }
        assertTrue(flinches(e))
    }

    @Test fun carinhoDesensina() {
        var e = .9f
        repeat(8) { e = expectPoke(e, poked = false) }
        assertEquals(false, flinches(e))
    }

    // ---- travessura ----
    private val calm = Drives(energy = .8f, boredom = .1f, longing = .1f, curiosity = .1f)

    @Test fun entediadoMasNaoCarenteAprontaAlgumas() {
        val counts = (0 until 400).map { decide(calm.copy(boredom = .75f, longing = .2f, energy = .45f), .5f, false, Random(it)) }
            .groupingBy { it }.eachCount()
        assertTrue((counts[Activity.PRANK] ?: 0) > 0)
    }

    @Test fun recadinhosExistem() {
        assertTrue(POSTIT_LINES.size >= 8)
    }
}
