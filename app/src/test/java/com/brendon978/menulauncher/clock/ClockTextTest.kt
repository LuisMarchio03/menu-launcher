package com.brendon978.menulauncher.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTextTest {
    @Test fun horaCheiaEmPonto() {
        assertEquals("três em ponto", timeInWords(3, 0))
        assertEquals("três em ponto", timeInWords(15, 0))
    }

    @Test fun meiaNoiteEMeioDia() {
        assertEquals("meia-noite", timeInWords(0, 0))
        assertEquals("meio-dia", timeInWords(12, 0))
        assertEquals("meio-dia e dez", timeInWords(12, 10))
        assertEquals("meia-noite e meia", timeInWords(0, 30))
    }

    @Test fun horasNoFeminino() {
        assertEquals("uma e cinco", timeInWords(13, 5))
        assertEquals("duas e vinte", timeInWords(2, 20))
    }

    @Test fun minutosCompostos() {
        assertEquals("dez e quinze", timeInWords(22, 15))
        assertEquals("dez e quarenta e cinco", timeInWords(10, 45))
        assertEquals("onze e cinquenta e nove", timeInWords(23, 59))
        assertEquals("sete e um", timeInWords(7, 1))
        assertEquals("oito e dezenove", timeInWords(8, 19))
    }
}
