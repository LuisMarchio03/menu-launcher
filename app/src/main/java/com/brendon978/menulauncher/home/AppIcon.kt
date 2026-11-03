package com.brendon978.menulauncher.home

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.icons.IconRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ícone de contorno desenhado fora da thread principal (a conversão do monocromático custa um pouco).
 * [stroke] multiplica a espessura do traço; [shadow] põe uma cópia escura e borrada atrás.
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
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bmp by produceState<ImageBitmap?>(null, entry.key, override, color, px, stroke) {
        value = withContext(Dispatchers.Default) { renderer.iconFor(entry, override, px, color, stroke) }
    }
    Box(Modifier.size(size)) {
        val b = bmp ?: return@Box
        if (shadow) {
            Image(
                b,
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.6f)),
                modifier = Modifier.size(size).offset(y = 1.dp).blur(2.dp),
            )
        }
        Image(b, contentDescription = null, modifier = Modifier.size(size))
    }
}
