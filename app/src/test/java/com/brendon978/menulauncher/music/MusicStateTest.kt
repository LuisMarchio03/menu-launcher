package com.brendon978.menulauncher.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicStateTest {
    @Test fun semSessaoNadaAparece() {
        assertNull(pickSession(emptyList()))
    }

    @Test fun prefereAQueEstaTocando() {
        val b = SessionInfo("b", playing = true)
        assertEquals(b, pickSession(listOf(SessionInfo("a", playing = false), b)))
    }

    @Test fun todasPausadasPegaAMaisRecente() {
        val a = SessionInfo("a", playing = false)
        assertEquals(a, pickSession(listOf(a, SessionInfo("b", playing = false))))
    }

    @Test fun tocandoSempreAparece() {
        assertEquals(true, shouldShowMusic(playing = true, pausedSince = null, now = 0))
    }

    @Test fun pausadaApareceAteDezMinutos() {
        val t0 = 1_000_000L
        assertEquals(true, shouldShowMusic(playing = false, pausedSince = t0, now = t0 + 9 * 60_000))
        assertEquals(false, shouldShowMusic(playing = false, pausedSince = t0, now = t0 + 10 * 60_000))
    }

    @Test fun pausadaSemMarcaNaoAparece() {
        assertEquals(false, shouldShowMusic(playing = false, pausedSince = null, now = 0))
    }

    @Test fun progressoAndaComOTempoQuandoToca() {
        // posição 30 s às 1000 ms do relógio; agora são 11000 ms: 40 s de 200 s = 20%
        assertEquals(.2f, songProgress(positionMs = 30_000, durationMs = 200_000, updatedAt = 1_000, now = 11_000, speed = 1f, playing = true)!!, .001f)
    }

    @Test fun pausadaNaoAnda() {
        assertEquals(.15f, songProgress(30_000, 200_000, 1_000, 99_000, 1f, playing = false)!!, .001f)
    }

    @Test fun naoPassaDoFim() {
        assertEquals(1f, songProgress(190_000, 200_000, 0, 60_000, 1f, true)!!, .001f)
    }

    @Test fun semDuracaoNaoMostra() {
        assertEquals(null, songProgress(30_000, 0, 0, 0, 1f, true))
        assertEquals(null, songProgress(-1, 200_000, 0, 0, 1f, true))
    }
}
