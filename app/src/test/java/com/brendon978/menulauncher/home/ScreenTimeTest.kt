package com.brendon978.menulauncher.home

import com.brendon978.menulauncher.home.ScreenEventKind.PAUSED
import com.brendon978.menulauncher.home.ScreenEventKind.RESUMED
import com.brendon978.menulauncher.home.ScreenEventKind.SCREEN_OFF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeTest {
    private val min = 60_000L

    @Test fun somaOsTrechosComAppAberto() {
        val eventos = listOf(
            ScreenEvent(10 * min, RESUMED, "a"), ScreenEvent(20 * min, PAUSED, "a"),
            ScreenEvent(30 * min, RESUMED, "b"), ScreenEvent(35 * min, PAUSED, "b"),
        )
        assertEquals(15 * min, screenMillis(eventos, 0, 100 * min))
    }

    @Test fun telaDivididaContaUmaVez() {
        val eventos = listOf(
            ScreenEvent(10 * min, RESUMED, "a"), ScreenEvent(12 * min, RESUMED, "b"),
            ScreenEvent(18 * min, PAUSED, "a"), ScreenEvent(20 * min, PAUSED, "b"),
        )
        assertEquals(10 * min, screenMillis(eventos, 0, 100 * min))
    }

    @Test fun appAindaAbertoContaAteAgora() {
        assertEquals(40 * min, screenMillis(listOf(ScreenEvent(60 * min, RESUMED, "a")), 0, 100 * min))
    }

    @Test fun appAbertoDesdeAntesDaMeiaNoite() {
        assertEquals(5 * min, screenMillis(listOf(ScreenEvent(15 * min, PAUSED, "a")), 10 * min, 100 * min))
    }

    @Test fun apagarATelaFechaOQueFicouAberto() {
        val eventos = listOf(ScreenEvent(10 * min, RESUMED, "a"), ScreenEvent(25 * min, SCREEN_OFF))
        assertEquals(15 * min, screenMillis(eventos, 0, 100 * min))
    }

    @Test fun eventosForaDeOrdem() {
        val eventos = listOf(ScreenEvent(20 * min, PAUSED, "a"), ScreenEvent(10 * min, RESUMED, "a"))
        assertEquals(10 * min, screenMillis(eventos, 0, 100 * min))
    }

    @Test fun semEventoNaoTemTempo() {
        assertEquals(0, screenMillis(emptyList(), 0, 100 * min))
    }

    @Test fun formato() {
        assertEquals("3h12 de tela hoje", formatScreenTime(192 * min))
        assertEquals("3h05 de tela hoje", formatScreenTime(185 * min))
        assertEquals("1h00 de tela hoje", formatScreenTime(60 * min))
        assertEquals("45min de tela hoje", formatScreenTime(45 * min + 59_000))
        assertEquals("0min de tela hoje", formatScreenTime(0))
    }

    @Test fun noMaximoUmaConsultaPorMinuto() {
        assertFalse(shouldRefreshScreenTime(10 * min + 59_999, 10 * min))
        assertTrue(shouldRefreshScreenTime(11 * min, 10 * min))
        assertTrue(shouldRefreshScreenTime(5 * min, 10 * min))
    }
}
