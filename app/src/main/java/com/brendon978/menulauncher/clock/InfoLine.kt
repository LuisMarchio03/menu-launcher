package com.brendon978.menulauncher.clock

/** Data, tempo de tela e o que mais vier numa linha só, separados por " · "; null se não sobrar nada. */
fun infoLine(vararg parts: String?): String? =
    parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(" · ").ifEmpty { null }
