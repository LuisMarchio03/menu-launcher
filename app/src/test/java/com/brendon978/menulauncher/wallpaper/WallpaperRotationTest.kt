package com.brendon978.menulauncher.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperRotationTest {
    private val hora = 3_600_000L

    @Test fun desligadoOuSemPastaNuncaTroca() {
        assertFalse(rotationDue(WallpaperInterval.OFF, "pasta", 0, 100 * hora))
        assertFalse(rotationDue(WallpaperInterval.HOURLY, "", 0, 100 * hora))
    }

    @Test fun esperaOIntervaloInteiro() {
        assertFalse(rotationDue(WallpaperInterval.HOURLY, "pasta", 10 * hora, 11 * hora - 1))
        assertTrue(rotationDue(WallpaperInterval.HOURLY, "pasta", 10 * hora, 11 * hora))
        assertFalse(rotationDue(WallpaperInterval.SIX_HOURS, "pasta", 10 * hora, 15 * hora))
        assertTrue(rotationDue(WallpaperInterval.SIX_HOURS, "pasta", 10 * hora, 16 * hora))
        assertFalse(rotationDue(WallpaperInterval.DAILY, "pasta", 10 * hora, 33 * hora))
        assertTrue(rotationDue(WallpaperInterval.DAILY, "pasta", 10 * hora, 34 * hora))
    }

    @Test fun primeiraVezTrocaLogo() {
        assertTrue(rotationDue(WallpaperInterval.DAILY, "pasta", 0, 100 * hora))
    }

    @Test fun relogioQueVoltouNaoTravaARotacao() {
        assertTrue(rotationDue(WallpaperInterval.DAILY, "pasta", 100 * hora, 50 * hora))
    }

    @Test fun soImagemEstaticaEntra() {
        assertTrue(isRotationImage("image/jpeg", "a.jpg"))
        assertTrue(isRotationImage("image/heic", "a.heic"))
        assertTrue(isRotationImage(null, "a.PNG"))
        assertFalse(isRotationImage("image/gif", "a.gif"))
        assertFalse(isRotationImage("video/mp4", "a.mp4"))
        assertFalse(isRotationImage("vnd.android.document/directory", "Fotos"))
        assertFalse(isRotationImage("image/svg+xml", "a.svg"))
    }

    @Test fun andaPelaPastaEDaAVolta() {
        val nomes = listOf("c.jpg", "A.jpg", "b.jpg")
        assertEquals(listOf("A.jpg", "b.jpg", "c.jpg"), rotationOrder(nomes, ""))
        assertEquals(listOf("b.jpg", "c.jpg"), rotationOrder(nomes, "A.jpg"))
        assertEquals(listOf("c.jpg", "A.jpg"), rotationOrder(nomes, "b.jpg"))
        assertEquals(listOf("A.jpg", "b.jpg"), rotationOrder(nomes, "c.jpg"))
    }

    @Test fun imagemApagadaNaoPerdeOLugar() {
        assertEquals(listOf("c.jpg", "a.jpg"), rotationOrder(listOf("a.jpg", "c.jpg"), "b.jpg"))
    }

    @Test fun pastaVaziaOuComAMesmaImagemNaoTemCandidata() {
        assertEquals(emptyList<String>(), rotationOrder(emptyList(), "a.jpg"))
        assertEquals(emptyList<String>(), rotationOrder(listOf("a.jpg"), "a.jpg"))
        assertEquals(listOf("a.jpg"), rotationOrder(listOf("a.jpg"), ""))
    }

    @Test fun nomeDaPasta() {
        assertEquals("Pictures/Fundos", folderLabel("primary:Pictures/Fundos"))
        assertEquals("Armazenamento", folderLabel("primary:"))
    }
}
