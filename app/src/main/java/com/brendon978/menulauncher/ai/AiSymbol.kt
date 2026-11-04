package com.brendon978.menulauncher.ai

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.home.LocalBorderWidth
import com.brendon978.menulauncher.pet.petObstacle
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * Símbolo flutuante da IA. Tocar: [onTap]. Segurar: [onHoldStart] e, ao soltar, [onHoldEnd].
 * Arrastar (antes do tempo de segurar): move; a posição final vai para [onMoved] em fração da tela.
 */
@Composable
fun AiSymbol(
    symbol: ImageBitmap?,
    color: Color,
    xFraction: Float,
    yFraction: Float,
    listening: Boolean,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onMoved: (Float, Float) -> Unit,
) {
    val size = 56.dp
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val sizePx = with(density) { size.toPx() }
        val maxX = (constraints.maxWidth - sizePx).coerceAtLeast(1f)
        val maxY = (constraints.maxHeight - sizePx).coerceAtLeast(1f)
        var drag by remember { mutableStateOf<Offset?>(null) }
        val pos = drag ?: Offset(clampFraction(xFraction) * maxX, clampFraction(yFraction) * maxY)
        LaunchedEffect(xFraction, yFraction) { drag = null }
        val currentPos = rememberUpdatedState(pos)
        val tap = rememberUpdatedState(onTap)
        val holdStart = rememberUpdatedState(onHoldStart)
        val holdEnd = rememberUpdatedState(onHoldEnd)
        val moved = rememberUpdatedState(onMoved)

        val pulse by rememberInfiniteTransition(label = "ai-pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "ai-pulse-scale",
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .size(size)
                .petObstacle("ai")
                .graphicsLayer {
                    val s = if (listening) pulse else 1f
                    scaleX = s
                    scaleY = s
                }
                .border(if (listening) LocalBorderWidth.current * 2 else LocalBorderWidth.current, color.copy(alpha = if (listening) 1f else 0.45f), CircleShape)
                .pointerInput(maxX, maxY) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        var travelled = Offset.Zero
                        // Até o tempo de segurar: ou solta (toque), ou mexe além do slop (arrastar).
                        val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Main)
                                val ch = ev.changes.first()
                                // lê antes de consumir: depois de consumido o movimento vem zerado
                                val moved = ch.positionChange()
                                ch.consume()
                                if (!ch.pressed) return@withTimeoutOrNull "tap"
                                travelled += moved
                                if (travelled.getDistance() > slop) return@withTimeoutOrNull "drag"
                            }
                            @Suppress("UNREACHABLE_CODE") ""
                        }
                        when (outcome) {
                            "tap" -> tap.value()
                            "drag" -> {
                                var p = currentPos.value + travelled
                                drag = p
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Main)
                                    val ch = ev.changes.first()
                                    val moved = ch.positionChange()
                                    ch.consume()
                                    if (!ch.pressed) break
                                    p = Offset(
                                        (p.x + moved.x).coerceIn(0f, maxX),
                                        (p.y + moved.y).coerceIn(0f, maxY),
                                    )
                                    drag = p
                                }
                                // drag só volta a null quando a posição salva chegar (sem pular de volta).
                                moved.value(p.x / maxX, p.y / maxY)
                            }
                            else -> {
                                // Segurou: ouve até soltar.
                                holdStart.value()
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Main)
                                    ev.changes.forEach { it.consume() }
                                    if (ev.changes.none { it.pressed }) break
                                }
                                holdEnd.value()
                            }
                        }
                    }
                },
        ) {
            if (symbol != null) Image(symbol, contentDescription = "IA", modifier = Modifier.size(34.dp))
        }
    }
}
