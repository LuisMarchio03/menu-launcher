package com.brendon978.menulauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class OrganizeTest {
    private val shop = AppEntry(AppKey("com.samsung.shop", "Main", 0), "Samsung Shop")
    private val calc = AppEntry(AppKey("com.calc", "Main", 0), "Calculadora")
    private val bixby = AppEntry(AppKey("com.bixby", "Main", 0), "Bixby")

    @Test fun nomeProprioSubstitui() {
        val out = organize(listOf(shop, calc), labels = mapOf(shop.key to "Shop"), hidden = emptySet())
        assertEquals(listOf("Shop", "Calculadora"), out.map { it.label })
    }

    @Test fun ocultoSomeDaLista() {
        assertEquals(listOf("Calculadora"), organize(listOf(bixby, calc), emptyMap(), hidden = setOf(bixby.key)).map { it.label })
    }

    @Test fun nomeEmBrancoNaoVale() {
        assertEquals("Samsung Shop", organize(listOf(shop), mapOf(shop.key to "  "), emptySet()).single().label)
    }
}
