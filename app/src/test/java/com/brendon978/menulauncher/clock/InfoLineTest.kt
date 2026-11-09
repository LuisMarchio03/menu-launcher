package com.brendon978.menulauncher.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InfoLineTest {
    @Test fun juntaComPontoNoMeio() {
        assertEquals("seg., 9 de novembro · 42min de tela hoje", infoLine("seg., 9 de novembro", "42min de tela hoje"))
    }

    @Test fun pulaOQueFaltar() {
        assertEquals("seg., 9 de novembro", infoLine("seg., 9 de novembro", null))
        assertEquals("42min", infoLine(null, " ", "42min"))
        assertNull(infoLine(null, null))
    }
}
