package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

object FavoritesOps {
    fun toggle(favs: List<AppKey>, key: AppKey): List<AppKey> =
        if (key in favs) favs - key else favs + key

    fun move(favs: List<AppKey>, from: Int, to: Int): List<AppKey> {
        if (from !in favs.indices || to !in favs.indices) return favs
        return favs.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun prune(s: LauncherSettings, installed: Set<AppKey>): LauncherSettings {
        val favs = s.favorites.filter { it in installed }
        val overrides = s.iconOverrides.filterKeys { it in installed }
        val hidden = s.hiddenApps.filter { it in installed }.toSet()
        return if (favs == s.favorites && overrides == s.iconOverrides && hidden == s.hiddenApps) s
        else s.copy(favorites = favs, iconOverrides = overrides, hiddenApps = hidden)
    }

    /** App escondido some da lista, da grade e da busca; por isso sai também dos favoritos. */
    fun setHidden(s: LauncherSettings, key: AppKey, hidden: Boolean): LauncherSettings =
        if (hidden) s.copy(hiddenApps = s.hiddenApps + key, favorites = s.favorites - key)
        else s.copy(hiddenApps = s.hiddenApps - key)
}
