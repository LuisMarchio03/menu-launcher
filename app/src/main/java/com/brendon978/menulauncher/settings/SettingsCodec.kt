package com.brendon978.menulauncher.settings

/**
 * Campos da personalização do fork, em texto: o [SettingsRepository] grava cada um numa chave
 * própria e o [Backup] leva todos para o arquivo.
 */
object SettingsCodec {
    class Field(
        val name: String,
        val write: (LauncherSettings) -> String,
        /** null = valor inválido; o ajuste atual fica. */
        val read: (LauncherSettings, String) -> LauncherSettings?,
    )

    private inline fun <reified E : Enum<E>> enumField(
        name: String,
        noinline get: (LauncherSettings) -> E,
        noinline set: (LauncherSettings, E) -> LauncherSettings,
    ) = Field(name, { get(it).name }, { s, v -> runCatching { enumValueOf<E>(v) }.getOrNull()?.let { set(s, it) } })

    private fun boolField(name: String, get: (LauncherSettings) -> Boolean, set: (LauncherSettings, Boolean) -> LauncherSettings) =
        Field(name, { get(it).toString() }, { s, v -> v.toBooleanStrictOrNull()?.let { set(s, it) } })

    private fun scale(
        name: String,
        get: (LauncherSettings) -> Float,
        max: Float = 2f,
        set: (LauncherSettings, Float) -> LauncherSettings,
    ) = Field(name, { get(it).toString() }, { s, v -> v.toFloatOrNull()?.let { set(s, it.coerceIn(0.5f, max)) } })

    val FIELDS: List<Field> = listOf(
        scale("clock_scale", { it.clockScale }) { s, f -> s.copy(clockScale = f) },
        scale("icon_scale", { it.iconScale }) { s, f -> s.copy(iconScale = f) },
        scale("text_scale", { it.textScale }) { s, f -> s.copy(textScale = f) },
        scale("row_scale", { it.rowScale }) { s, f -> s.copy(rowScale = f) },
        scale("bar_scale", { it.barScale }) { s, f -> s.copy(barScale = f) },
        scale("stroke_scale", { it.strokeScale }, max = 3f) { s, f -> s.copy(strokeScale = f) },
        scale("border_scale", { it.borderScale }, max = 3f) { s, f -> s.copy(borderScale = f) },
        enumField("clock_style", { it.clockStyle }) { s, v -> s.copy(clockStyle = v) },
        enumField("date_style", { it.dateStyle }) { s, v -> s.copy(dateStyle = v) },
        boolField("clock_seconds", { it.clockSeconds }) { s, v -> s.copy(clockSeconds = v) },
        enumField("clock_align", { it.clockAlign }) { s, v -> s.copy(clockAlign = v) },
        enumField("hour_format", { it.hourFormat }) { s, v -> s.copy(hourFormat = v) },
        enumField("clock_tap", { it.clockTap }) { s, v -> s.copy(clockTap = v) },
        Field("second_zone", { it.secondZone.orEmpty() }, { s, v -> s.copy(secondZone = v.ifEmpty { null }) }),
        enumField("color_source", { it.colorSource }) { s, v -> s.copy(colorSource = v) },
        boolField("text_shadow", { it.textShadow }) { s, v -> s.copy(textShadow = v) },
        enumField("font", { it.font }) { s, v -> s.copy(font = v) },
        // Leitura pura: quem troca o lado na tela usa withBarSide; aqui aiX já vem certo junto.
        enumField("bar_side", { it.barSide }) { s, v -> s.copy(barSide = v) },
        Field("grid_columns", { it.gridColumns.toString() }, { s, v -> v.toIntOrNull()?.takeIf { it == 0 || it in 3..7 }?.let { s.copy(gridColumns = it) } }),
        boolField("grid_labels", { it.gridLabels }) { s, v -> s.copy(gridLabels = v) },
        enumField("music_style", { it.musicStyle }) { s, v -> s.copy(musicStyle = v) },
        enumField("quick_style", { it.quickStyle }) { s, v -> s.copy(quickStyle = v) },
        Field("detail_alpha", { it.detailAlpha.toString() }, { s, v -> v.toFloatOrNull()?.let { s.copy(detailAlpha = it.coerceIn(0.25f, 1f)) } }),
        enumField("label_style", { it.labelStyle }) { s, v -> s.copy(labelStyle = v) },
        enumField("empty_note", { it.emptyNote }) { s, v -> s.copy(emptyNote = v) },
        boolField("bar_curved", { it.barCurved }) { s, v -> s.copy(barCurved = v) },
        enumField("bar_reveal", { it.barReveal }) { s, v -> s.copy(barReveal = v) },
        enumField("app_weight", { it.appWeight }) { s, v -> s.copy(appWeight = v) },
        scale("spacing", { it.spacing }) { s, f -> s.copy(spacing = f) },
        boolField("blur_overlays", { it.blurOverlays }) { s, v -> s.copy(blurOverlays = v) },
        boolField("icon_halo", { it.iconHalo }) { s, v -> s.copy(iconHalo = v) },
        Field("edge_veil", { it.edgeVeil.toString() }, { s, v -> v.toFloatOrNull()?.let { s.copy(edgeVeil = it.coerceIn(0f, 1f)) } }),
        boolField("compact_info", { it.compactInfo }) { s, v -> s.copy(compactInfo = v) },
        enumField("quick_reveal", { it.quickReveal }) { s, v -> s.copy(quickReveal = v) },
        boolField("glitch_calm", { it.glitchCalm }) { s, v -> s.copy(glitchCalm = v) },
        boolField("ai_ring", { it.aiRing }) { s, v -> s.copy(aiRing = v) },
        scale("ai_scale", { it.aiScale }, max = 1.5f) { s, f -> s.copy(aiScale = f) },
        enumField("initial_style", { it.initialStyle }) { s, v -> s.copy(initialStyle = v) },
        boolField("motion", { it.motion }) { s, v -> s.copy(motion = v) },
        enumField("list_align", { it.listAlign }) { s, v -> s.copy(listAlign = v) },
        enumField("block_position", { it.blockPosition }) { s, v -> s.copy(blockPosition = v) },
        enumField("icon_shape", { it.iconShape }) { s, v -> s.copy(iconShape = v) },
        Field("plate_light", { it.plateLight.toString() }, { s, v -> v.toFloatOrNull()?.let { s.copy(plateLight = it.coerceIn(0f, 1f)) } }),
        boolField("show_labels", { it.showLabels }) { s, v -> s.copy(showLabels = v) },
        boolField("favorite_pages", { it.favoritePages }) { s, v -> s.copy(favoritePages = v) },
        Field(
            "category_overrides",
            { s -> s.categoryOverrides.entries.joinToString("\n") { "${it.key.encode()}\t${it.value}" } },
            { s, v ->
                s.copy(
                    categoryOverrides = v.lines().mapNotNull { line ->
                        val (k, c) = line.split('\t').takeIf { it.size == 2 } ?: return@mapNotNull null
                        com.brendon978.menulauncher.apps.AppKey.decode(k)?.let { it to c }
                    }.toMap(),
                )
            },
        ),
        Field("custom_themes", { LookThemes.encode(it.customThemes) }, { s, v -> s.copy(customThemes = LookThemes.decode(v)) }),
    )
}
