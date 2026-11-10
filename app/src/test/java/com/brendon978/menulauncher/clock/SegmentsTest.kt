package com.brendon978.menulauncher.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentsTest {
    // bits: a=topo, b=alto à direita, c=baixo à direita, d=base, e=baixo à esquerda, f=alto à esquerda, g=meio
    @Test fun tabelaPadraoDosSeteSegmentos() {
        assertEquals(0x3F, segmentsOf('0'))
        assertEquals(0x06, segmentsOf('1'))
        assertEquals(0x5B, segmentsOf('2'))
        assertEquals(0x66, segmentsOf('4'))
        assertEquals(0x7F, segmentsOf('8'))
        assertEquals(0x6F, segmentsOf('9'))
    }

    @Test fun foraDosDigitosNaoAcende() {
        assertEquals(0, segmentsOf(':'))
    }
}
