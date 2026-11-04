package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GlitchLifeTest {
    @Test fun inclinacoes() {
        assertEquals(Tilt.LEVEL, tiltOf(.2f, 9.5f, 1f))
        assertEquals(Tilt.LEVEL, tiltOf(.1f, .1f, 9.8f))
        assertEquals(Tilt.LEFT, tiltOf(8f, 3f, 2f))
        assertEquals(Tilt.RIGHT, tiltOf(-8f, 3f, 2f))
        assertEquals(Tilt.UPSIDE_DOWN, tiltOf(0f, -9f, 1f))
    }

    @Test fun chacoalharPedeTresTrancos() {
        var m = ShakeMeter()
        assertFalse(m.sample(25f, 1_000).also { m = it.first }.second)
        assertFalse(m.sample(25f, 1_200).also { m = it.first }.second)
        assertTrue(m.sample(25f, 1_400).also { m = it.first }.second)
        assertFalse("descansa antes de contar de novo", m.sample(25f, 1_600).second)
    }

    @Test fun movimentoFracoOuTrancosEspalhadosNaoContam() {
        var m = ShakeMeter()
        repeat(6) { i -> assertFalse(m.sample(12f, 1_000L + i * 100).also { x -> m = x.first }.second) }
        repeat(6) { i -> assertFalse(m.sample(25f, 10_000L + i * 2_000).also { x -> m = x.first }.second) }
    }

    @Test fun comentarios() {
        assertEquals("2h no Instagram, hein?", returnComment("com.instagram.android", "Instagram", 125 * 60_000L, Random(1)))
        assertEquals("25 minutos no Banco, hein?", returnComment("com.banco", "Banco", 25 * 60_000L, Random(1)))
        assertNotNull(returnComment("com.instagram.android", "Instagram", 60_000L, Random(1)))
        assertNull("app qualquer e rápido não rende fala", returnComment("com.banco", "Banco", 60_000L, Random(1)))
    }

    @Test fun comentarioNaoEnche() {
        assertFalse((0 until 100).any { commentNow(0L, 60_000L, Random(it)) })
        val vezes = (0 until 1_000).count { commentNow(0L, 20 * 60_000L, Random(it)) }
        assertTrue(vezes in 300..500)
    }

    @Test fun aniversario() {
        assertEquals(5 to 3, parseBirthday(" 5/3 "))
        assertEquals(25 to 12, parseBirthday("25-12"))
        assertNull(parseBirthday("31/13"))
        assertNull(parseBirthday("ontem"))
        assertNull(parseBirthday(""))
    }

    @Test fun datas() {
        assertEquals(SpecialDay.HALLOWEEN, specialDay(31, 10, ""))
        assertEquals(SpecialDay.CHRISTMAS, specialDay(24, 12, ""))
        assertEquals(SpecialDay.NEW_YEAR, specialDay(31, 12, ""))
        assertEquals(SpecialDay.NEW_YEAR, specialDay(1, 1, ""))
        assertEquals("aniversário vence", SpecialDay.BIRTHDAY, specialDay(25, 12, "25/12"))
        assertNull(specialDay(4, 11, ""))
    }

    @Test fun oQueEleVeste() {
        assertEquals(Accessory.NIGHT_GEAR, wornAccessory(Face.SLEEP, 23, false, true, SpecialDay.CHRISTMAS, Accessory.CAP))
        assertEquals(Accessory.HEADPHONES, wornAccessory(Face.NORMAL, 14, false, true, SpecialDay.CHRISTMAS, Accessory.CAP))
        assertEquals(Accessory.SANTA_HAT, wornAccessory(Face.NORMAL, 14, false, false, SpecialDay.CHRISTMAS, Accessory.CAP))
        assertEquals(Accessory.CAP, wornAccessory(Face.NORMAL, 14, false, false, null, Accessory.CAP))
    }
}
