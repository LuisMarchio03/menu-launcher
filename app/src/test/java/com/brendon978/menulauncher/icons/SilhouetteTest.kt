package com.brendon978.menulauncher.icons

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class SilhouetteTest {
    private val size = 64
    private val clear = 0x00000000
    private val blue = 0xFF2050C0.toInt()
    private val white = 0xFFFFFFFF.toInt()

    private fun image(color: (x: Int, y: Int) -> Int) = IntArray(size * size) { color(it % size, it / size) }
    private fun mask(on: (x: Int, y: Int) -> Boolean) = BooleanArray(size * size) { on(it % size, it / size) }

    // um "L" de traço grosso: ocupa pouco da caixa em volta, então é o próprio desenho
    private val ell = { x: Int, y: Int -> (x in 16..23 && y in 12..51) || (x in 16..47 && y in 44..51) }
    private val cross = { x: Int, y: Int -> (x in 28..35 && y in 16..47) || (x in 16..47 && y in 28..35) }

    @Test fun formaSobreFundoTransparenteEhODesenho() {
        val img = image { x, y -> if (ell(x, y)) blue else clear }
        assertArrayEquals(mask(ell), Silhouette.extract(img, size))
    }

    @Test fun placaCheiaUsaOQueDifereDaCorDeFundo() {
        val img = image { x, y -> if (cross(x, y)) white else blue }
        assertArrayEquals(mask(cross), Silhouette.extract(img, size))
    }

    @Test fun placaRedondaTambem() {
        val inCircle = { x: Int, y: Int -> (x - 31.5) * (x - 31.5) + (y - 31.5) * (y - 31.5) <= 31.0 * 31.0 }
        val square = { x: Int, y: Int -> x in 24..39 && y in 24..39 }
        val img = image { x, y -> if (!inCircle(x, y)) clear else if (square(x, y)) white else blue }
        assertArrayEquals(mask(square), Silhouette.extract(img, size))
    }

    @Test fun fotoOuRuidoNaoViraDesenho() {
        val rnd = Random(7)
        assertNull(Silhouette.extract(image { _, _ -> 0xFF000000.toInt() or rnd.nextInt(0xFFFFFF) }, size))
    }

    @Test fun placaLisaSemSimboloNaoViraDesenho() {
        assertNull(Silhouette.extract(image { _, _ -> blue }, size))
    }

    @Test fun vazioOuPontinhoNaoViraDesenho() {
        assertNull(Silhouette.extract(image { _, _ -> clear }, size))
        assertNull(Silhouette.extract(image { x, y -> if (x == 30 && y == 30) blue else clear }, size))
    }

    @Test fun muitosPedacosNaoViramDesenho() {
        // texto ou ilustração miúda: sairia sujo, fica a letra
        val pecas = { x: Int, y: Int -> x % 8 in 0..3 && y % 8 in 0..3 && x in 8..55 && y in 8..55 }
        assertNull(Silhouette.extract(image { x, y -> if (pecas(x, y)) blue else clear }, size))
    }

    @Test fun ciscoSoltoSai() {
        val img = image { x, y -> if (ell(x, y) || (x == 60 && y == 2)) blue else clear }
        assertArrayEquals(mask(ell), Silhouette.extract(img, size))
    }
}
