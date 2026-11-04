package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.apps.LetterIndex

/** Um item da grade: um app solto ou uma pasta com apps. */
sealed interface GridItem {
    val label: String
    data class App(val entry: AppEntry) : GridItem { override val label get() = entry.label }
    data class Folder(val name: String, val apps: List<AppEntry>) : GridItem { override val label get() = name }
}

object FolderOps {
    /** Coloca o app na pasta (e tira de qualquer outra). Nome em branco não cria nada. */
    fun put(folders: Map<String, List<AppKey>>, name: String, key: AppKey): Map<String, List<AppKey>> {
        val n = name.trim()
        if (n.isEmpty()) return folders
        val without = remove(folders, key)
        return without + (n to (without[n].orEmpty() + key))
    }

    /** Tira o app da pasta em que estiver; pasta vazia some. */
    fun remove(folders: Map<String, List<AppKey>>, key: AppKey): Map<String, List<AppKey>> =
        folders.mapValues { (_, v) -> v - key }.filterValues { it.isNotEmpty() }

    /** Grade: apps fora de pastas e as pastas (só com apps que existem), tudo em ordem alfabética. */
    fun gridItems(apps: List<AppEntry>, folders: Map<String, List<AppKey>>): List<GridItem> {
        val byKey = apps.associateBy { it.key }
        val inFolder = folders.values.flatten().toSet()
        val loose = apps.filter { it.key !in inFolder }.map { GridItem.App(it) }
        val packed = folders.mapNotNull { (name, keys) ->
            val members = keys.mapNotNull { byKey[it] }
            if (members.isEmpty()) null else GridItem.Folder(name, LetterIndex.sorted(members))
        }
        return (loose + packed).sortedWith { x, y -> LetterIndex.collator.compare(x.label, y.label) }
    }
}
