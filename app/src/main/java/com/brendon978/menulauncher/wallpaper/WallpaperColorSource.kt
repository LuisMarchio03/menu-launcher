package com.brendon978.menulauncher.wallpaper

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Cor do traço tirada do papel de parede da tela inicial, atualizada quando ele muda.
 * Papel animado sem cores calculadas: usa a paleta do sistema (a da One UI, que segue o papel de parede).
 * O aviso de troca do sistema pode não chegar (o launcher estava em segundo plano), por isso [refresh].
 */
class WallpaperColorSource(private val context: Context) {
    private val wm = WallpaperManager.getInstance(context)
    private val _color = MutableStateFlow(compute(wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)))
    val color: StateFlow<Int> = _color

    private val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
        if (which and WallpaperManager.FLAG_SYSTEM != 0) _color.value = compute(colors)
    }

    init {
        wm.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
    }

    fun close() = wm.removeOnColorsChangedListener(listener)

    /** Relê as cores do papel atual. Bloqueia: chamar fora da thread principal. */
    fun refresh() {
        _color.value = compute(wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM))
    }

    private fun compute(colors: WallpaperColors?): Int = autoStroke(colors, context.getColor(android.R.color.system_accent1_500))
}

/** A cor do traço automático para estas cores do papel; [fallback] quando o papel não informa cor. */
fun autoStroke(colors: WallpaperColors?, fallback: Int): Int = legibleStroke(colors?.let(::wallpaperBase) ?: fallback, supportsDarkText(colors))

/** Papel claro: o sistema diz que texto escuro se lê melhor nele. */
fun supportsDarkText(colors: WallpaperColors?): Boolean =
    colors != null && colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0

/** A cor de [strokeBase] entre as três principais do papel (a tela inicial e o bloqueio usam esta). */
fun wallpaperBase(colors: WallpaperColors): Int =
    strokeBase(listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor).map { it.toArgb() })
