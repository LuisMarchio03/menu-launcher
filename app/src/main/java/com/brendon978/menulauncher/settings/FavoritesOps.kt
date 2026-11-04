package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.apps.searchApps

object FavoritesOps {
    fun toggle(favs: List<AppKey>, key: AppKey): List<AppKey> =
        if (key in favs) favs - key else favs + key

    /** Só adiciona: toque repetido, antes de a lista atualizar, não desfaz. */
    fun add(favs: List<AppKey>, key: AppKey): List<AppKey> = if (key in favs) favs else favs + key

    fun move(favs: List<AppKey>, from: Int, to: Int): List<AppKey> {
        if (from !in favs.indices || to !in favs.indices) return favs
        return favs.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun prune(s: LauncherSettings, installed: Set<AppKey>): LauncherSettings {
        val favs = s.favorites.filter { it in installed }
        val overrides = s.iconOverrides.filterKeys { it in installed }
        val labels = s.customLabels.filterKeys { it in installed }
        val hidden = s.hiddenApps.filter { it in installed }.toSet()
        val folders = s.folders.mapValues { (_, v) -> v.filter { it in installed } }.filterValues { it.isNotEmpty() }
        val usage = s.usage.filterKeys { it in installed }
        return if (favs == s.favorites && overrides == s.iconOverrides && labels == s.customLabels && hidden == s.hiddenApps && folders == s.folders && usage == s.usage) s
        else s.copy(favorites = favs, iconOverrides = overrides, customLabels = labels, hiddenApps = hidden, folders = folders, usage = usage)
    }

    /** Apps que ainda dá para favoritar (nem favoritos, nem escondidos): em ordem alfabética, ou os que a [query] achar. */
    fun candidates(apps: Collection<AppEntry>, s: LauncherSettings, query: String): List<AppEntry> {
        val taken = s.favorites.toSet() + s.hiddenApps
        val free = apps.filter { it.key !in taken }
        return if (query.isBlank()) LetterIndex.sorted(free) else searchApps(free, query)
    }

    /** App escondido some da lista e da grade (a busca ainda acha); por isso sai também dos favoritos. */
    fun setHidden(s: LauncherSettings, key: AppKey, hidden: Boolean): LauncherSettings =
        if (hidden) s.copy(hiddenApps = s.hiddenApps + key, favorites = s.favorites - key)
        else s.copy(hiddenApps = s.hiddenApps - key)
}
