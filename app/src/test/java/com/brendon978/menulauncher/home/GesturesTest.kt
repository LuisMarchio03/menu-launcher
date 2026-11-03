package com.brendon978.menulauncher.home

import com.brendon978.menulauncher.settings.GestureAction
import com.brendon978.menulauncher.settings.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GesturesTest {
    @Test fun arrastoCurtoNaoConta() {
        assertNull(swipeOf(totalDy = 40f, thresholdPx = 100f))
        assertNull(swipeOf(totalDy = -99f, thresholdPx = 100f))
    }

    @Test fun direcaoPeloSinal() {
        assertEquals(Swipe.DOWN, swipeOf(totalDy = 150f, thresholdPx = 100f))
        assertEquals(Swipe.UP, swipeOf(totalDy = -150f, thresholdPx = 100f))
    }

    @Test fun acaoVemDosAjustes() {
        val s = LauncherSettings(swipeDown = GestureAction.SETTINGS, swipeUp = GestureAction.NONE)
        assertEquals(GestureAction.SETTINGS, s.actionFor(Swipe.DOWN))
        assertEquals(GestureAction.NONE, s.actionFor(Swipe.UP))
    }

    @Test fun padraoNotificacoesEBusca() {
        val s = LauncherSettings()
        assertEquals(GestureAction.NOTIFICATIONS, s.actionFor(Swipe.DOWN))
        assertEquals(GestureAction.SEARCH, s.actionFor(Swipe.UP))
        assertEquals(GestureAction.NONE, s.doubleTap)
    }
}
