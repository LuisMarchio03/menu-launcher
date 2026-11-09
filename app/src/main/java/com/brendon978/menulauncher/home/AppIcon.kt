package com.brendon978.menulauncher.home

import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Offset
import com.brendon978.menulauncher.music.MediaListener
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.pet.GlitchSignals
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.wallpaper.haloFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ícone de contorno desenhado fora da thread principal (a conversão do monocromático custa um pouco).
 * [stroke] multiplica a espessura do traço; [shadow] põe atrás uma cópia borrada no tom oposto ([haloFor]).
 */
@Composable
fun AppIcon(
    entry: AppEntry,
    override: String?,
    color: Int,
    size: Dp,
    renderer: IconRenderer,
    stroke: Float = 1f,
    shadow: Boolean = false,
    tracksBadge: Boolean = false,
) {
    val px = with(LocalDensity.current) { (if (LocalIconShape.current == com.brendon978.menulauncher.settings.IconShape.OUTLINE) size else size * .72f).roundToPx() }
    val bmp by produceState<ImageBitmap?>(null, entry.key, override, color, px, stroke, renderer.initialStyle) {
        value = withContext(Dispatchers.Default) { renderer.iconFor(entry, override, px, color, stroke) }
    }
    // bolinha de notificação: só aparece se o app tem notificação "de verdade"
    val badges by MediaListener.badges.collectAsState()
    val pkg = entry.key.packageName
    val badged = pkg in badges && pkg !in GlitchSignals.eaten
    // só a lista da tela inicial marca onde a bolinha está: a busca abre por cima e tem outra posição
    val tracked = badged && tracksBadge
    if (tracked) DisposableEffect(pkg) { onDispose { GlitchSignals.badgeSpots.remove(pkg) } }
    // círculo ou quadrado translúcido em volta: o ícone encolhe um pouco para caber com folga
    val shape = LocalIconShape.current
    val inner = if (shape == com.brendon978.menulauncher.settings.IconShape.OUTLINE) size else size * .72f
    val backing = when (shape) {
        com.brendon978.menulauncher.settings.IconShape.OUTLINE -> Modifier
        com.brendon978.menulauncher.settings.IconShape.CIRCLE -> Modifier.background(Color(color).copy(alpha = .16f), androidx.compose.foundation.shape.CircleShape)
        com.brendon978.menulauncher.settings.IconShape.SQUARE -> Modifier.background(Color(color).copy(alpha = .16f), androidx.compose.foundation.shape.RoundedCornerShape(size * .28f))
    }
    Box(Modifier.size(size).then(backing).then(if (tracked) Modifier.onGloballyPositioned { GlitchSignals.badgeSpots[pkg] = it.boundsInRoot() } else Modifier), contentAlignment = Alignment.Center) {
        bmp?.let { b ->
            if (shadow) {
                Image(
                    b,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(haloFor(color)).copy(alpha = 0.6f)),
                    modifier = Modifier.size(inner).offset(y = 1.dp).blur(2.dp),
                )
            }
            Image(b, contentDescription = null, modifier = Modifier.size(inner))
        }
        if (badged) {
            Canvas(Modifier.size(size)) {
                val r = this.size.minDimension * .17f
                val c = Offset(this.size.width - r * .6f, r * .6f)
                drawCircle(Color(0xFF0C0C0E), r + 1.6f, c)
                drawCircle(Color(color), r, c)
            }
        }
    }
}
