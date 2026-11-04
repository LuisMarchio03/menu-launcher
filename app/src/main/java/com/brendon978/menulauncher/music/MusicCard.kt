package com.brendon978.menulauncher.music

import androidx.compose.foundation.Image
import android.os.SystemClock
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.brendon978.menulauncher.home.LocalBorderWidth
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Topo: só mostra o que está tocando. Os controles ficam embaixo (MusicControls). */
@Composable
fun MusicCard(state: MusicState, color: Color, modifier: Modifier = Modifier, textSize: TextUnit = 12.sp) {
    val border = LocalBorderWidth.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .border(border, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .songProgressOutline(state, color, border * 2)
            .padding(10.dp),
    ) {
        val art = remember(state.art) { state.art?.asImageBitmap() }
        if (art != null) {
            Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
        } else {
            Box(Modifier.size(36.dp).border(border, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp)))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(state.title, color = color, fontSize = textSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(state.artist, state.appLabel).joinToString(" · "),
                color = color.copy(alpha = 0.6f),
                fontSize = textSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Contorno de progresso: uma linha vai sendo desenhada em volta da caixa conforme a música passa,
 * começando no meio do topo e andando no sentido do relógio. Fechou a volta, acabou a música.
 */
private fun Modifier.songProgressOutline(state: MusicState, color: Color, width: Dp): Modifier = composed {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    // só anda quadro a quadro enquanto toca (e a tela está visível)
    LaunchedEffect(state.playing, state.updatedAt) {
        now = SystemClock.elapsedRealtime()
        while (state.playing) withFrameMillis { now = SystemClock.elapsedRealtime() }
    }
    val progress = songProgress(state.positionMs, state.durationMs, state.updatedAt, now, state.speed, state.playing)
    drawWithContent {
        drawContent()
        if (progress == null || progress <= 0f) return@drawWithContent
        val stroke = width.toPx()
        val r = 14.dp.toPx()
        val inset = stroke / 2
        val w = size.width - stroke
        val h = size.height - stroke
        // caminho da caixa arredondada começando no meio do topo
        val path = Path().apply {
            moveTo(inset + w / 2, inset)
            lineTo(inset + w - r, inset)
            arcTo(Rect(inset + w - 2 * r, inset, inset + w, inset + 2 * r), -90f, 90f, false)
            lineTo(inset + w, inset + h - r)
            arcTo(Rect(inset + w - 2 * r, inset + h - 2 * r, inset + w, inset + h), 0f, 90f, false)
            lineTo(inset + r, inset + h)
            arcTo(Rect(inset, inset + h - 2 * r, inset + 2 * r, inset + h), 90f, 90f, false)
            lineTo(inset, inset + r)
            arcTo(Rect(inset, inset, inset + 2 * r, inset + 2 * r), 180f, 90f, false)
            lineTo(inset + w / 2, inset)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val done = Path()
        measure.getSegment(0f, measure.length * progress, done, true)
        drawPath(done, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
