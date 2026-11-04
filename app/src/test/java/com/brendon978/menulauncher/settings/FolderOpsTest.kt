package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderOpsTest {
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val c = AppKey("c", "C", 0)
    private fun e(k: AppKey, l: String) = AppEntry(k, l)

    @Test fun colocarNaPastaTiraDasOutras() {
        val f = FolderOps.put(mapOf("Jogos" to listOf(a)), "Bancos", a)
        assertEquals(mapOf("Bancos" to listOf(a)), f)
    }

    @Test fun tirarDaUltimaApagaAPasta() {
        assertEquals(emptyMap<String, List<AppKey>>(), FolderOps.remove(mapOf("Jogos" to listOf(a)), a))
    }

    @Test fun gradeMisturaPastasEAppsEmOrdem() {
        val apps = listOf(e(a, "Zap"), e(b, "Banco"), e(c, "Calc"))
        val items = FolderOps.gridItems(apps, mapOf("Jogos" to listOf(a)))
        assertEquals(listOf("Banco", "Calc", "Jogos"), items.map { it.label })
        assertEquals(listOf(a), (items.last() as GridItem.Folder).apps.map { it.key })
    }

    @Test fun pastaComAppsQueSumiramSome() {
        val items = FolderOps.gridItems(listOf(e(b, "Banco")), mapOf("Jogos" to listOf(a)))
        assertEquals(listOf("Banco"), items.map { it.label })
    }

    @Test fun nomeDePastaVazioNaoVale() {
        assertEquals(emptyMap<String, List<AppKey>>(), FolderOps.put(emptyMap(), "  ", a))
    }
}
