package com.brendon978.menulauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeAutoTest {
    private val fixa = 0xFF7FB7A4.toInt()
    private val dia = 0xFFFFFFFF.toInt()
    private val noite = 0xFFC9A27E.toInt()

    @Test fun desligadaEhSempreACorFixa() {
        val s = LauncherSettings(strokeColor = fixa, strokeDay = dia, strokeNight = noite)
        assertEquals(fixa, effectiveStroke(s, 3, 0xFF112233.toInt()))
        assertEquals(fixa, effectiveStroke(s, 12, null))
    }

    @Test fun diaDas7As19() {
        assertFalse(isDayHour(6))
        assertTrue(isDayHour(7))
        assertTrue(isDayHour(18))
        assertFalse(isDayHour(19))
        val s = LauncherSettings(strokeAuto = StrokeAuto.BY_TIME, strokeDay = dia, strokeNight = noite)
        assertEquals(dia, effectiveStroke(s, 7, null))
        assertEquals(noite, effectiveStroke(s, 19, null))
        assertEquals(noite, effectiveStroke(s, 2, null))
    }

    @Test fun papelDeParedeSemCorCaiNaCorFixa() {
        val s = LauncherSettings(strokeAuto = StrokeAuto.BY_WALLPAPER, strokeColor = fixa)
        assertEquals(fixa, effectiveStroke(s, 12, null))
    }

    @Test fun papelDeParedeEscuroClareiaAteLer() {
        val s = LauncherSettings(strokeAuto = StrokeAuto.BY_WALLPAPER)
        for (c in listOf(0xFF000000.toInt(), 0xFF101820.toInt(), 0xFF3A0A0A.toInt(), 0xFF00008B.toInt())) {
            assertTrue(contrastOnDark(effectiveStroke(s, 12, c)) >= 7.0)
        }
    }

    @Test fun corQueJaLeNaoMuda() {
        assertEquals(0xFFE6E1D6.toInt(), legibleOnDark(0xFFE6E1D6.toInt()))
    }

    @Test fun clarearMantemOTom() {
        val azul = legibleOnDark(0xFF102080.toInt())
        val r = azul shr 16 and 0xFF
        val b = azul and 0xFF
        assertTrue(b > r)
    }

    @Test fun transparenteViraOpaco() {
        assertEquals(0xFF, legibleOnDark(0x40FFFFFF) ushr 24)
    }
}
