package com.brendon978.menulauncher.wallpaper

/** De quanto em quanto tempo o papel de parede da tela inicial muda. */
enum class WallpaperInterval(val label: String, val millis: Long) {
    OFF("Desligado", 0),
    HOURLY("A cada hora", 3_600_000),
    SIX_HOURS("A cada 6 horas", 6 * 3_600_000),
    DAILY("Todo dia", 24 * 3_600_000),
}

private val NAME_ORDER = compareBy<String, String>(String.CASE_INSENSITIVE_ORDER) { it }.thenBy { it }

private val EXTRA_MIME = setOf("image/heic", "image/heif")

/** Chegou a hora de trocar? Relógio que voltou no tempo conta como sim, para a rotação não ficar parada. */
fun rotationDue(interval: WallpaperInterval, folder: String, lastChange: Long, now: Long): Boolean =
    interval != WallpaperInterval.OFF && folder.isNotEmpty() && (now < lastChange || now - lastChange >= interval.millis)

/** Só imagem estática: GIF, vídeo e subpasta ficam de fora. HEIC é o formato de foto da Samsung. */
fun isRotationImage(mime: String?, name: String?): Boolean =
    classify(mime, name) == WallpaperKind.Static || mime?.lowercase() in EXTRA_MIME

/**
 * As candidatas por ordem de nome, começando na que vem depois de [last] e dando a volta.
 * A própria [last] fica de fora: pasta com uma imagem só não reaplica a mesma.
 */
fun rotationOrder(names: List<String>, last: String): List<String> {
    val sorted = names.distinct().sortedWith(NAME_ORDER).filter { it != last }
    val next = sorted.indexOfFirst { NAME_ORDER.compare(it, last) > 0 }
    return if (next <= 0) sorted else sorted.drop(next) + sorted.take(next)
}

/** "primary:Pictures/Fundos" vira "Pictures/Fundos"; a raiz do armazenamento vira "Armazenamento". */
fun folderLabel(treeDocumentId: String): String =
    treeDocumentId.substringAfter(':').ifBlank { "Armazenamento" }
