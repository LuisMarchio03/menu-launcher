package com.brendon978.menulauncher.icons

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutlineTracerTest {
    private val size = 192

    private fun square(from: Int, to: Int) = IntArray(size * size) { i ->
        val x = i % size; val y = i / size
        if (x in from until to && y in from until to) 255 else 0
    }

    @Test fun quadradoCheioViraBorda() {
        val out = OutlineTracer.trace(square(46, 146), size, 14)
        assertFalse("centro deve ficar vazio", out[96 * size + 96])
        assertTrue("borda esquerda deve ser traço", out[96 * size + 46])
        assertFalse("longe do quadrado fica vazio", out[96 * size + 10])
    }

    @Test fun transparenteDaVazio() {
        assertTrue(OutlineTracer.trace(IntArray(size * size), size, 14).none { it })
    }

    @Test fun cantoDoTracoEhRedondo() {
        // um ponto só: o traço em volta dele é um disco, não um quadrado
        val ponto = IntArray(size * size).also { it[96 * size + 96] = 255 }
        val out = OutlineTracer.trace(ponto, size, 8)
        assertTrue("na reta, a 4px, é traço", out[96 * size + 100])
        assertFalse("na diagonal, a 4px de cada lado (5,7px), já não é", out[100 * size + 100])
    }
}
