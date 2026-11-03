package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsCodecTest {
    private val a = AppKey("com.spotify.music", "com.spotify.music.MainActivity", 0)
    private val b = AppKey("com.whatsapp", "com.whatsapp.Main", 150)

    private val custom = LauncherSettings(
        mode = HomeMode.GRID,
        favorites = listOf(a, b),
        strokeColor = 0xFFA0C4FF.toInt(),
        iconOverrides = mapOf(a to "spotify"),
        showMusic = false,
        showPet = false,
        clockScale = 1.3f,
        iconScale = 1.25f,
        textScale = 0.9f,
        rowScale = 1.1f,
        barScale = 1.25f,
        clockStyle = ClockStyle.WORDS,
        dateStyle = DateStyle.NUMERIC,
        secondZone = "Europe/Lisbon",
        colorSource = ColorSource.WALLPAPER,
        strokeScale = 1.5f,
        textShadow = true,
        font = FontChoice.SERIF,
        barSide = BarSide.LEFT,
        gridColumns = 5,
        gridLabels = false,
        hiddenApps = setOf(b),
        swipeDown = GestureAction.SETTINGS,
        swipeUp = GestureAction.NONE,
        doubleTap = GestureAction.LOCK,
    )

    @Test fun idaEVoltaPreservaOVisual() {
        val back = SettingsCodec.decode(SettingsCodec.encode(custom), LauncherSettings(firstRunDone = true))!!
        // aiX é do aparelho; só espelha porque a barra mudou de lado (teste abaixo).
        assertEquals(custom.copy(firstRunDone = true, aiX = back.aiX), back)
    }

    @Test fun barraQueMudaDeLadoLevaOSimboloDaIaJunto() {
        val r = SettingsCodec.decode(SettingsCodec.encode(custom), LauncherSettings(aiX = 0.7f))!!
        assertEquals(BarSide.LEFT, r.barSide)
        assertEquals(0.3f, r.aiX, 1e-6f)
    }

    @Test fun iaEPrimeiraAberturaFicamDoAparelho() {
        val exported = SettingsCodec.encode(custom.copy(aiPackage = "iris", aiActivity = "iris.Main", aiX = 0.1f))
        val here = LauncherSettings(firstRunDone = true, aiPackage = "eloy", aiActivity = "eloy.Main", aiX = 0.4f, barSide = BarSide.LEFT)
        val r = SettingsCodec.decode(exported, here)!!
        assertEquals("eloy", r.aiPackage)
        assertEquals("eloy.Main", r.aiActivity)
        assertEquals(0.4f, r.aiX)
        assertEquals(true, r.firstRunDone)
    }

    @Test fun arquivoQueNaoEDoLauncherEhRecusado() {
        assertNull(SettingsCodec.decode("qualquer coisa\nmode=GRID", LauncherSettings()))
    }

    @Test fun valorInvalidoMantemOAtual() {
        val text = "${SettingsCodec.HEADER}\nmode=XYZ\nclock_scale=abc\ngrid_columns=4\nchave_nova=1"
        val r = SettingsCodec.decode(text, LauncherSettings(mode = HomeMode.GRID))!!
        assertEquals(HomeMode.GRID, r.mode)
        assertEquals(1f, r.clockScale)
        assertEquals(4, r.gridColumns)
    }
}
