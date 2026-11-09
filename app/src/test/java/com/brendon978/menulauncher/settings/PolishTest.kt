package com.brendon978.menulauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PolishTest {
    @Test fun opcoesNovasVaoNoBackup() {
        val s = LauncherSettings(
            edgeVeil = 0.6f, compactInfo = false, quickReveal = QuickReveal.ALWAYS, font = FontChoice.OUTFIT,
            glitchCalm = false, aiRing = true, aiScale = 1.2f, initialStyle = InitialStyle.CIRCLE, motion = false,
        )
        val v = Backup.decode(Backup.encode(s))!!
        assertEquals(0.6f, v.edgeVeil)
        assertEquals(false, v.compactInfo)
        assertEquals(QuickReveal.ALWAYS, v.quickReveal)
        assertEquals(FontChoice.OUTFIT, v.font)
        assertEquals(false, v.glitchCalm)
        assertEquals(true, v.aiRing)
        assertEquals(1.2f, v.aiScale)
        assertEquals(InitialStyle.CIRCLE, v.initialStyle)
        assertEquals(false, v.motion)
    }

    @Test fun veuForaDaFaixaFicaNoLimite() {
        val text = Backup.encode(LauncherSettings()).replace(Regex("look.edge_veil=.*"), "look.edge_veil=7")
        assertEquals(1f, Backup.decode(text)!!.edgeVeil)
    }
}
