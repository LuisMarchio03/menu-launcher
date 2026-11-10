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

    @Test fun placaEClaridadeVaoNoBackup() {
        val v = Backup.decode(Backup.encode(LauncherSettings(iconShape = IconShape.PLATE, plateLight = 0.65f)))!!
        assertEquals(IconShape.PLATE, v.iconShape)
        assertEquals(0.65f, v.plateLight)
        val fora = Backup.encode(LauncherSettings()).replace(Regex("look.plate_light=.*"), "look.plate_light=-3")
        assertEquals(0f, Backup.decode(fora)!!.plateLight)
    }

    @Test fun veuForaDaFaixaFicaNoLimite() {
        val text = Backup.encode(LauncherSettings()).replace(Regex("look.edge_veil=.*"), "look.edge_veil=7")
        assertEquals(1f, Backup.decode(text)!!.edgeVeil)
    }
}

class LayoutOptionsTest {
    @Test fun opcoesDeFormaVaoNoBackup() {
        val key = com.brendon978.menulauncher.apps.AppKey("com.whatsapp", "com.whatsapp.Main", 0)
        val s = LauncherSettings(
            mode = HomeMode.FAN, listAlign = ListAlign.CENTER, blockPosition = BlockPosition.MIDDLE,
            iconShape = IconShape.CIRCLE, showLabels = false, favoritePages = true, categoryOverrides = mapOf(key to "GAMES"),
        )
        val v = Backup.decode(Backup.encode(s))!!
        assertEquals(HomeMode.FAN, v.mode)
        assertEquals(ListAlign.CENTER, v.listAlign)
        assertEquals(BlockPosition.MIDDLE, v.blockPosition)
        assertEquals(IconShape.CIRCLE, v.iconShape)
        assertEquals(false, v.showLabels)
        assertEquals(true, v.favoritePages)
        assertEquals(mapOf(key to "GAMES"), v.categoryOverrides)
    }
}
