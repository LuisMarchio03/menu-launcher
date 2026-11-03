package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

/**
 * Ajustes em texto ("chave=valor" por linha), para exportar num celular e importar no outro.
 * Fica de fora o que é do aparelho: IA conectada, posição do símbolo e a primeira abertura.
 * [FIELDS] também é o que o [SettingsRepository] grava para os ajustes de personalização.
 */
object SettingsCodec {
    const val HEADER = "menu-launcher-ajustes 1"

    class Field(
        val name: String,
        val write: (LauncherSettings) -> String,
        /** null = valor inválido; o ajuste atual fica. */
        val read: (LauncherSettings, String) -> LauncherSettings?,
        /** Já gravado pelo repositório com chave tipada própria (anterior a este arquivo). */
        val legacy: Boolean = false,
    )

    private inline fun <reified E : Enum<E>> enumOf(v: String): E? = runCatching { enumValueOf<E>(v) }.getOrNull()

    private inline fun <reified E : Enum<E>> enumField(
        name: String,
        noinline get: (LauncherSettings) -> E,
        noinline set: (LauncherSettings, E) -> LauncherSettings,
        legacy: Boolean = false,
    ) = Field(name, { get(it).name }, { s, v -> enumOf<E>(v)?.let { set(s, it) } }, legacy)

    private fun boolField(
        name: String,
        get: (LauncherSettings) -> Boolean,
        legacy: Boolean = false,
        set: (LauncherSettings, Boolean) -> LauncherSettings,
    ) = Field(name, { get(it).toString() }, { s, v -> v.toBooleanStrictOrNull()?.let { set(s, it) } }, legacy)

    private fun keys(v: String): List<AppKey> = v.split(',').filter { it.isNotEmpty() }.mapNotNull { AppKey.decode(it) }

    private fun scale(name: String, get: (LauncherSettings) -> Float, set: (LauncherSettings, Float) -> LauncherSettings) =
        Field(name, { get(it).toString() }, { s, v -> v.toFloatOrNull()?.let { set(s, it.coerceIn(SCALE_MIN, SCALE_MAX)) } })

    private const val SCALE_MIN = 0.5f
    private const val SCALE_MAX = 2f

    val FIELDS: List<Field> = listOf(
        enumField("mode", { it.mode }, { s, v -> s.copy(mode = v) }, legacy = true),
        Field("favorites", { s -> s.favorites.joinToString(",") { it.encode() } }, { s, v -> s.copy(favorites = keys(v)) }, legacy = true),
        Field("stroke_color", { "%08X".format(it.strokeColor) }, { s, v -> v.toLongOrNull(16)?.let { s.copy(strokeColor = it.toInt()) } }, legacy = true),
        Field(
            "icon_overrides",
            { s -> s.iconOverrides.entries.joinToString(",") { (k, n) -> "${k.encode()}>$n" } },
            { s, v ->
                val map = v.split(',').filter { it.isNotEmpty() }.mapNotNull { e ->
                    val i = e.lastIndexOf('>')
                    if (i < 0) null else AppKey.decode(e.substring(0, i))?.let { it to e.substring(i + 1) }
                }.toMap()
                s.copy(iconOverrides = map)
            },
            legacy = true,
        ),
        boolField("show_music", { it.showMusic }, legacy = true) { s, v -> s.copy(showMusic = v) },
        boolField("show_pet", { it.showPet }, legacy = true) { s, v -> s.copy(showPet = v) },
        scale("clock_scale", { it.clockScale }) { s, f -> s.copy(clockScale = f) },
        scale("icon_scale", { it.iconScale }) { s, f -> s.copy(iconScale = f) },
        scale("text_scale", { it.textScale }) { s, f -> s.copy(textScale = f) },
        scale("row_scale", { it.rowScale }) { s, f -> s.copy(rowScale = f) },
        scale("bar_scale", { it.barScale }) { s, f -> s.copy(barScale = f) },
        scale("stroke_scale", { it.strokeScale }) { s, f -> s.copy(strokeScale = f) },
        enumField("clock_style", { it.clockStyle }, { s, v -> s.copy(clockStyle = v) }),
        enumField("date_style", { it.dateStyle }, { s, v -> s.copy(dateStyle = v) }),
        Field("second_zone", { it.secondZone.orEmpty() }, { s, v -> s.copy(secondZone = v.ifEmpty { null }) }),
        enumField("color_source", { it.colorSource }, { s, v -> s.copy(colorSource = v) }),
        boolField("text_shadow", { it.textShadow }) { s, v -> s.copy(textShadow = v) },
        enumField("font", { it.font }, { s, v -> s.copy(font = v) }),
        enumField("bar_side", { it.barSide }, { s, v -> s.withBarSide(v) }),
        Field("grid_columns", { it.gridColumns.toString() }, { s, v -> v.toIntOrNull()?.takeIf { it == 0 || it in 3..7 }?.let { s.copy(gridColumns = it) } }),
        boolField("grid_labels", { it.gridLabels }) { s, v -> s.copy(gridLabels = v) },
        Field("hidden_apps", { s -> s.hiddenApps.joinToString(",") { it.encode() } }, { s, v -> s.copy(hiddenApps = keys(v).toSet()) }),
        enumField("swipe_down", { it.swipeDown }, { s, v -> s.copy(swipeDown = v) }),
        enumField("swipe_up", { it.swipeUp }, { s, v -> s.copy(swipeUp = v) }),
        enumField("double_tap", { it.doubleTap }, { s, v -> s.copy(doubleTap = v) }),
    )

    fun encode(s: LauncherSettings): String =
        (listOf(HEADER) + FIELDS.map { "${it.name}=${it.write(s)}" }).joinToString("\n")

    /** Aplica o arquivo sobre [base] (os ajustes atuais). Arquivo que não começa com [HEADER]: null. */
    fun decode(text: String, base: LauncherSettings): LauncherSettings? {
        val lines = text.lines()
        if (lines.firstOrNull()?.trim() != HEADER) return null
        val byName = FIELDS.associateBy { it.name }
        return lines.drop(1).fold(base) { s, line ->
            val i = line.indexOf('=')
            val field = if (i < 0) null else byName[line.substring(0, i).trim()]
            field?.read?.invoke(s, line.substring(i + 1).trim()) ?: s
        }
    }
}
