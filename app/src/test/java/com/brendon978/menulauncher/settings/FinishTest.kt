package com.brendon978.menulauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinishTest {
    @Test fun acabamentoVaiNoBackup() {
        val s = LauncherSettings(
            quickStyle = QuickStyle.CIRCLES, detailAlpha = 0.4f, labelStyle = LabelStyle.HIDDEN,
            emptyNote = EmptyNote.TEXT, barCurved = true, appWeight = AppWeight.NORMAL, spacing = 1.4f,
        )
        val volta = Backup.decode(Backup.encode(s))!!
        assertEquals(QuickStyle.CIRCLES, volta.quickStyle)
        assertEquals(0.4f, volta.detailAlpha)
        assertEquals(LabelStyle.HIDDEN, volta.labelStyle)
        assertEquals(EmptyNote.TEXT, volta.emptyNote)
        assertEquals(true, volta.barCurved)
        assertEquals(AppWeight.NORMAL, volta.appWeight)
        assertEquals(1.4f, volta.spacing)
    }

    @Test fun desfoqueESombraDosDesenhosVaoNoBackup() {
        val volta = Backup.decode(Backup.encode(LauncherSettings(blurOverlays = false, iconHalo = false)))!!
        assertEquals(false, volta.blurOverlays)
        assertEquals(false, volta.iconHalo)
    }

    @Test fun detalhesNuncaSomemDeVez() {
        val text = Backup.encode(LauncherSettings()).replace(Regex("look.detail_alpha=.*"), "look.detail_alpha=0.01")
        assertEquals(0.25f, Backup.decode(text)!!.detailAlpha)
    }

    @Test fun rotuloDasSecoes() {
        assertEquals("Favoritos", sectionLabel("Favoritos", LabelStyle.NORMAL))
        assertEquals("FAVORITOS", sectionLabel("Favoritos", LabelStyle.CAPS))
        assertNull(sectionLabel("Favoritos", LabelStyle.HIDDEN))
    }
}
