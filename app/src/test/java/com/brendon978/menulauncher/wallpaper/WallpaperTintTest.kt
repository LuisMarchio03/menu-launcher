package com.brendon978.menulauncher.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class WallpaperTintTest {
    /** Luminância relativa da WCAG 2.x. */
    private fun luminance(c: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(c shr 16 and 0xFF) + 0.7152 * ch(c shr 8 and 0xFF) + 0.0722 * ch(c and 0xFF)
    }

    private val samples = listOf(0xFF0000FF, 0xFFFFFF00, 0xFF1B3A2F, 0xFFE8D5B5, 0xFF808080, 0xFF000000, 0xFFFFFFFF)
        .map { it.toInt() }

    @Test fun papelEscuroDaTracoClaroComContrasteAAA() {
        // contraste 7:1 contra preto: (L + 0.05) / 0.05 >= 7  ->  L >= 0.30
        samples.forEach { assertTrue("%08X".format(it), luminance(legibleStroke(it, darkText = false)) >= 0.30) }
    }

    @Test fun papelClaroDaTracoEscuroComContrasteAAA() {
        // contraste 7:1 contra branco: 1.05 / (L + 0.05) >= 7  ->  L <= 0.10
        samples.forEach { assertTrue("%08X".format(it), luminance(legibleStroke(it, darkText = true)) <= 0.10) }
    }

    @Test fun mantemOMatiz() {
        val blue = legibleStroke(0xFF0000FF.toInt(), darkText = false)
        val (r, g, b) = Triple(blue shr 16 and 0xFF, blue shr 8 and 0xFF, blue and 0xFF)
        assertTrue(b > r && b > g)
        val gray = legibleStroke(0xFF808080.toInt(), darkText = true)
        assertTrue(abs((gray shr 16 and 0xFF) - (gray and 0xFF)) <= 2)
    }

    @Test fun fundoPretoCedeParaACorDeVerdade() {
        val orange = 0xFFE06010.toInt()
        assertEquals(orange, strokeBase(listOf(0xFF050505.toInt(), 0xFF202020.toInt(), orange)))
    }

    @Test fun quaseCinzaEscuroNaoContaComoCor() {
        // em HSL este quase preto tem saturação alta, mas a olho é preto
        val green = 0xFF20A050.toInt()
        assertEquals(green, strokeBase(listOf(0xFF0A0508.toInt(), green)))
    }

    @Test fun corPrincipalComMatizFica() {
        val navy = 0xFF204080.toInt()
        assertEquals(navy, strokeBase(listOf(navy, 0xFFE06010.toInt())))
    }

    @Test fun papelSemCorFicaNaPrincipal() {
        val gray = 0xFF404040.toInt()
        assertEquals(gray, strokeBase(listOf(gray, 0xFF808080.toInt(), 0xFFC0C0C0.toInt())))
    }

    @Test fun saiOpaco() {
        assertEquals(0xFF, legibleStroke(0x400000FF, darkText = false) ushr 24)
    }
}
