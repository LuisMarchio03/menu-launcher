package com.brendon978.menulauncher.settingsui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsIndexTest {
    @Test fun achaSemAcentoEMaiuscula() {
        val hits = SettingsIndex.search("RELOGIO")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.page == "LOOK" })
    }

    @Test fun cadaOpcaoApontaParaAPaginaDela() {
        assertEquals("HOME", SettingsIndex.search("nota").first().page)
        assertEquals("SYSTEM", SettingsIndex.search("backup").first().page)
    }

    @Test fun vazioNaoMostraNada() {
        assertTrue(SettingsIndex.search("  ").isEmpty())
        assertTrue(SettingsIndex.search("xyzzy").isEmpty())
    }
}
