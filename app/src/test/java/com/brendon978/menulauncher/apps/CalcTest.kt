package com.brendon978.menulauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    @Test fun contasBasicas() {
        assertEquals("84", calc("12*7"))
        assertEquals("14", calc("2+3*4"))
        assertEquals("20", calc("(2+3)*4"))
        assertEquals("2,5", calc("5/2"))
        assertEquals("-3", calc("2-5"))
    }

    @Test fun aceitaVirgulaEX() {
        assertEquals("3", calc("1,5*2"))
        assertEquals("6", calc("2x3"))
    }

    @Test fun porcentagem() {
        assertEquals("15", calc("10%*150"))
    }

    @Test fun textoNaoEConta() {
        assertNull(calc("spotify"))
        assertNull(calc("12"))
        assertNull(calc("2+"))
        assertNull(calc(""))
    }

    @Test fun divisaoPorZeroNaoQuebra() {
        assertNull(calc("5/0"))
    }

    @Test fun numerosPequenosEGrandesSemNotacaoCientifica() {
        assertEquals("0,0005", calc("1/2000"))
        assertEquals("33333333,333333", calc("100000000/3"))
        assertEquals("25000000000000", calc("5000000*5000000"))
        assertEquals("12345678900,5", calc("12345678900+0,5"))
        assertEquals("0,3", calc("0,1+0,2"))
    }
}
