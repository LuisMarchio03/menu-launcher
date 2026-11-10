package com.brendon978.menulauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LookThemesTest {
    private val verde = 0xFF7DFF9B.toInt()
    private val terminal = LookTheme("Terminal", ClockStyle.TERMINAL, verde, StrokeAuto.OFF, FontChoice.MONO, textShadow = false, DateStyle.NUMERIC, ClockAlign.START)

    @Test fun aplicarTrocaSoAAparencia() {
        val antes = LauncherSettings(favorites = emptyList(), showMusic = false, clockScale = 1.3f)
        val depois = antes.applyTheme(terminal)
        assertEquals(ClockStyle.TERMINAL, depois.clockStyle)
        assertEquals(verde, depois.strokeColor)
        assertEquals(FontChoice.MONO, depois.font)
        assertEquals(DateStyle.NUMERIC, depois.dateStyle)
        // o resto fica como estava
        assertEquals(1.3f, depois.clockScale)
        assertFalse(depois.showMusic)
    }

    @Test fun oTemaAplicadoEhReconhecido() {
        val s = LauncherSettings().applyTheme(terminal)
        assertTrue(s.uses(terminal))
        assertFalse(s.copy(font = FontChoice.SERIF).uses(terminal))
    }

    @Test fun salvarOAtualGuardaOQueEstaNaTela() {
        val s = LauncherSettings().applyTheme(terminal).copy(textShadow = true)
        val meu = s.toTheme("Meu")
        assertEquals("Meu", meu.name)
        assertTrue(s.uses(meu))
        assertTrue(meu.textShadow)
    }

    @Test fun temasSalvosIdaEVolta() {
        val lista = listOf(terminal, terminal.copy(name = "Com\ttab e = igual", strokeAuto = StrokeAuto.BY_WALLPAPER))
        assertEquals(lista, LookThemes.decode(LookThemes.encode(lista)))
        assertEquals(emptyList<LookTheme>(), LookThemes.decode(""))
    }

    @Test fun linhaEstragadaSaiSemDerrubarAsOutras() {
        val texto = LookThemes.encode(listOf(terminal)) + "\nlixo\tNAO_EXISTE"
        assertEquals(listOf(terminal), LookThemes.decode(texto))
    }

    @Test fun temasSalvosVaoNoBackup() {
        val s = LauncherSettings(customThemes = listOf(terminal))
        assertEquals(listOf(terminal), Backup.decode(Backup.encode(s))!!.customThemes)
    }

    @Test fun prontosTemNomesDiferentes() {
        assertEquals(LookThemes.PRESETS.size, LookThemes.PRESETS.map { it.name }.toSet().size)
        assertTrue(LookThemes.PRESETS.any { it.clockStyle == ClockStyle.WEEKDAY })
    }
}
