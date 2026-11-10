package com.brendon978.menulauncher.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class PlateColorsTest {
    // WCAG 2: luminância relativa e razão de contraste, escritas aqui à parte do código testado
    private fun lum(c: Int): Double {
        fun ch(v: Int) = (v / 255.0).let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * ch(c shr 16 and 0xFF) + 0.7152 * ch(c shr 8 and 0xFF) + 0.0722 * ch(c and 0xFF)
    }
    private fun contrast(a: Int, b: Int) = (max(lum(a), lum(b)) + 0.05) / (min(lum(a), lum(b)) + 0.05)
    private fun r(c: Int) = c shr 16 and 0xFF
    private fun g(c: Int) = c shr 8 and 0xFF
    private fun b(c: Int) = c and 0xFF

    private val atlasBlue = 0xFF78A6FF.toInt()

    @Test fun placaEscuraNoMatizDoDestaque() {
        val p = plateColors(atlasBlue, 0.3f)
        assertTrue("azul por cima: ${Integer.toHexString(p.fill)}", b(p.fill) > g(p.fill) && g(p.fill) > r(p.fill))
        assertTrue(lum(p.fill) < 0.03)
        assertEquals(0xFF, p.fill ushr 24)
    }

    @Test fun simboloFicaNaCorDoDestaqueQuandoJaContrasta() {
        val p = plateColors(atlasBlue, 0.3f)
        assertEquals(atlasBlue, p.symbol)
    }

    @Test fun destaqueEscuroClareiaAteContrastar() {
        val navy = 0xFF1A2A55.toInt()
        val p = plateColors(navy, 0.3f)
        assertNotEquals(navy, p.symbol)
        assertTrue(contrast(p.symbol, p.fill) >= 4.5)
        assertTrue("mesmo matiz azul", b(p.symbol) > r(p.symbol))
    }

    @Test fun placaMaisClaraContinuaLegivel() {
        val dark = plateColors(atlasBlue, 0f)
        val light = plateColors(atlasBlue, 1f)
        assertTrue(lum(light.fill) > lum(dark.fill))
        assertTrue(contrast(light.symbol, light.fill) >= 4.5)
    }

    @Test fun destaqueCinzaDaPlacaNeutra() {
        val p = plateColors(0xFFBBBBBB.toInt(), 0.3f)
        assertTrue(max(r(p.fill), max(g(p.fill), b(p.fill))) - min(r(p.fill), min(g(p.fill), b(p.fill))) <= 4)
    }

    @Test fun aroTranslucidoNaCorDoSimbolo() {
        val p = plateColors(atlasBlue, 0.3f)
        assertEquals(p.symbol and 0xFFFFFF, p.ring and 0xFFFFFF)
        assertTrue((p.ring ushr 24) in 1..127)
    }
}
