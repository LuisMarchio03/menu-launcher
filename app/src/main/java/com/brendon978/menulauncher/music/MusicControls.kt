package com.brendon978.menulauncher.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.home.LocalBorderWidth
import com.brendon978.menulauncher.home.LocalLineScale
import androidx.compose.foundation.border

/** ⏮ ⏯ ⏭ em contorno, na zona do polegar; alvos de 48dp. */
@Composable
fun MusicControls(
    playing: Boolean,
    color: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    /** Aro em volta do tocar/pausar (cartão com capa). */
    ringPlay: Boolean = false,
) {
    val lw = LocalLineScale.current
    Row(
        // SpaceEvenly cabe tanto na largura toda quanto ao lado do relógio.
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth(),
    ) {
        ControlButton(onPrevious) { skip(color, forward = false, lw) }
        ControlButton(onPlayPause, ring = if (ringPlay) color else null) { if (playing) pause(color, lw) else play(color, lw) }
        ControlButton(onNext) { skip(color, forward = true, lw) }
    }
}

@Composable
private fun ControlButton(onClick: () -> Unit, ring: Color? = null, draw: DrawScope.() -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (ring != null) Modifier.border(LocalBorderWidth.current * 1.5f, ring.copy(alpha = .6f), CircleShape) else Modifier)
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.size(22.dp), onDraw = draw)
    }
}

private fun DrawScope.stroke(lw: Float) = Stroke(width = size.width * 0.08f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.play(color: Color, lw: Float) {
    val w = size.width
    drawPath(Path().apply { moveTo(w * .25f, w * .15f); lineTo(w * .85f, w * .5f); lineTo(w * .25f, w * .85f); close() }, color, style = stroke(lw))
}

private fun DrawScope.pause(color: Color, lw: Float) {
    val w = size.width
    drawLine(color, Offset(w * .35f, w * .18f), Offset(w * .35f, w * .82f), w * .08f * lw, StrokeCap.Round)
    drawLine(color, Offset(w * .65f, w * .18f), Offset(w * .65f, w * .82f), w * .08f * lw, StrokeCap.Round)
}

private fun DrawScope.skip(color: Color, forward: Boolean, lw: Float) {
    val w = size.width
    fun x(f: Float) = if (forward) w * f else w * (1 - f)
    drawPath(Path().apply { moveTo(x(.2f), w * .2f); lineTo(x(.7f), w * .5f); lineTo(x(.2f), w * .8f); close() }, color, style = stroke(lw))
    drawLine(color, Offset(x(.82f), w * .2f), Offset(x(.82f), w * .8f), w * .08f * lw, StrokeCap.Round)
}
