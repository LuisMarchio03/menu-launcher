package com.brendon978.menulauncher.home

import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import kotlin.math.cos
import kotlin.math.sin

/** Até tantos ícones no arco de dentro do leque; o resto vai para um arco maior, por fora. */
const val FAN_INNER = 6

/**
 * Onde cada ícone do leque fica, em relação ao canto de baixo (direito ou esquerdo): x e y negativos sobem
 * e entram na tela. Os ícones se espalham por um quarto de círculo de raio [radius]; passando de [FAN_INNER],
 * os seguintes vão num arco 1,6x maior.
 */
fun fanSlots(count: Int, radius: Float, rightSide: Boolean): List<Pair<Float, Float>> {
    fun ring(n: Int, r: Float): List<Pair<Float, Float>> = (0 until n).map { i ->
        // de 180° (rente ao chão) a 270° (em pé), com folga nas pontas
        val t = if (n == 1) .5f else i / (n - 1f)
        val a = Math.toRadians(180.0 + 8 + t * (90 - 16))
        val x = (cos(a) * r).toFloat()
        (if (rightSide) x else -x) to (sin(a) * r).toFloat()
    }
    val inner = minOf(count, FAN_INNER)
    return ring(inner, radius) + ring(count - inner, radius * 1.6f)
}

/** Páginas do modo uma mão: os favoritos e depois cada pasta com app, em ordem alfabética. */
fun favoritePages(favorites: List<AppEntry>, folders: Map<String, List<AppKey>>, byKey: Map<AppKey, AppEntry>): List<Pair<String, List<AppEntry>>> =
    listOf("Favoritos" to favorites) +
        folders.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (name, keys) -> name to keys.mapNotNull { byKey[it] } }
            .filter { it.second.isNotEmpty() }
