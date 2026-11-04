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

    private fun compute(colors: WallpaperColors?): Int {
        val base = colors?.let { c -> strokeBase(listOfNotNull(c.primaryColor, c.secondaryColor, c.tertiaryColor).map { it.toArgb() }) }
            ?: context.getColor(android.R.color.system_accent1_500)
        val darkText = colors != null && colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        return legibleStroke(base, darkText)
    }
}
