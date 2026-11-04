package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

/** Quanto você usa cada app (aberto pelo launcher). Cada abertura soma 1 e as outras esfriam um pouco. */
object Usage {
    private const val COOL = .97f
    private const val FORGET = .05f

    fun bump(u: Map<AppKey, Float>, key: AppKey): Map<AppKey, Float> =
        (u.mapValues { it.value * COOL }.filterValues { it >= FORGET } + (key to ((u[key] ?: 0f) * COOL + 1f)))

    fun top(u: Map<AppKey, Float>, n: Int, exclude: Set<AppKey>): List<AppKey> =
        u.filterKeys { it !in exclude }.entries.sortedByDescending { it.value }.take(n).map { it.key }
}
