package com.brendon978.menulauncher.home

import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.StrokeAuto
import com.brendon978.menulauncher.settings.effectiveStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

/** A cor do traço que vale agora. Com a opção desligada não há relógio nem escuta do papel de parede. */
@Composable
fun rememberStrokeColor(settings: LauncherSettings): Int {
    val hour by produceState(currentHour(), settings.strokeAuto) {
        if (settings.strokeAuto != StrokeAuto.BY_TIME) return@produceState
        while (true) {
            value = currentHour()
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }
    val wallpaper = if (settings.strokeAuto == StrokeAuto.BY_WALLPAPER) rememberWallpaperColor() else null
    return effectiveStroke(settings, hour, wallpaper)
}

private fun currentHour() = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

/** Cor principal do papel de parede da tela inicial; lê fora da thread principal e acompanha as trocas. */
@Composable
private fun rememberWallpaperColor(): Int? {
    val context = LocalContext.current
    val manager = remember { WallpaperManager.getInstance(context) }
    var color by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(manager) {
        color = withContext(Dispatchers.IO) {
            runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() }.getOrNull()
        }
    }
    DisposableEffect(manager) {
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) color = colors?.primaryColor?.toArgb()
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }
    return color
}
