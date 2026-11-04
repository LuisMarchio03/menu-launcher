package com.brendon978.menulauncher.icons

import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.apps.LetterIndex

sealed interface IconSource {
    data class Lawnicon(val name: String) : IconSource
    /** Ícone desenhado por nós (assets/proprios), para apps que o Lawnicons não tem. */
    data class Own(val name: String) : IconSource
    data class Monochrome(val key: AppKey) : IconSource
    data class Initial(val letter: Char) : IconSource
}

/** Ordem: troca manual -> Lawnicons -> ícone próprio -> monocromático em contorno -> inicial. */
fun resolveIcon(
    key: AppKey,
    label: String,
    override: String?,
    filter: AppFilter,
    ownIcons: Map<String, String> = OWN_ICONS,
    hasMonochrome: (AppKey) -> Boolean,
): IconSource {
    override?.let { return IconSource.Lawnicon(it) }
    filter.drawableFor(key.packageName, key.activityName)?.let { return IconSource.Lawnicon(it) }
    ownIcons[key.packageName]?.let { return IconSource.Own(it) }
    if (hasMonochrome(key)) return IconSource.Monochrome(key)
    return IconSource.Initial(initialOf(label))
}

internal fun initialOf(label: String): Char {
    val letter = LetterIndex.letterOf(label)
    if (letter != LetterIndex.OTHER) return letter
    val c = label.trimStart().firstOrNull() ?: return '?'
    return if (c.isSurrogate()) '?' else c.uppercaseChar()
}

/** Pacote -> nome do SVG em assets/proprios (desenhados no mesmo traço do Lawnicons). */
val OWN_ICONS: Map<String, String> = mapOf(
    "com.brendon.iris" to "iris",
    "com.brendon978.menulauncher" to "glitch",
    "com.brendon.buildgarage" to "garagem",
    "com.brendon.apenasumnerd" to "nerd",
    "com.brendon978.patronus" to "patronus",
    "br.com.starapp.patronusbarbearia" to "barbearia",
    "br.com.sicredi.app" to "banco",
    "br.gov.serpro.sougov" to "gov",
    "com.hdqwalls.hdqwalls1" to "papel_de_parede",
    "com.netflix.NGP.Kamo" to "controle",
    "com.promedico.promedico" to "saude",
    "com.roachgames.tofudrifter" to "carro",
    "org.chromium.webapk.a62931ee669ce881f_v2" to "hangar",
)
