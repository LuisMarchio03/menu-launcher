package com.brendon978.menulauncher.clock

import com.brendon978.menulauncher.settings.HourFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockFaceTest {
    @Test fun vinteEQuatroHorasComZeroNaFrente() {
        assertEquals("13:12", clockFace(13, 12, 5, HourFormat.H24, systemIs24h = false, showSeconds = false).text)
        assertEquals("09:05", clockFace(9, 5, 0, HourFormat.H24, systemIs24h = false, showSeconds = false).text)
    }

    @Test fun dozeHorasSemZeroEMeiaNoiteEhDoze() {
        assertEquals("1:12", clockFace(13, 12, 5, HourFormat.H12, systemIs24h = true, showSeconds = false).text)
        assertEquals("12:30", clockFace(0, 30, 0, HourFormat.H12, systemIs24h = true, showSeconds = false).text)
        assertNull(clockFace(13, 12, 5, HourFormat.H12, systemIs24h = true, showSeconds = false).suffix)
    }

    @Test fun amPmESegundos() {
        assertEquals("1:12:05 PM", clockFace(13, 12, 5, HourFormat.H12_AMPM, systemIs24h = true, showSeconds = true).text)
        assertEquals("AM", clockFace(9, 0, 0, HourFormat.H12_AMPM, systemIs24h = true, showSeconds = false).suffix)
    }

    @Test fun doSistemaSegueOCelular() {
        assertEquals("13:12", clockFace(13, 12, 0, HourFormat.SYSTEM, systemIs24h = true, showSeconds = false).text)
        assertEquals("1:12", clockFace(13, 12, 0, HourFormat.SYSTEM, systemIs24h = false, showSeconds = false).text)
    }
}
