package com.brendon978.menulauncher.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Quanto do arrasto a linha acompanha: só uma dica de que o gesto pegou. */
private const val FOLLOW = .35f
private val SWIPE_DISTANCE = 72.dp

/** Tem de andar a distância inteira; quem volta o dedo antes de soltar desiste. */
fun swipeOpensActions(dragX: Float, distance: Float): Boolean = abs(dragX) >= distance

/**
 * Deslizar para o lado chama [onSwipe]. O detector só assume depois de o dedo andar na horizontal:
 * toque e toque longo seguem com o clique, e arrasto vertical segue com a rolagem da lista.
 * Fica por dentro do clique e por fora da linha, que acompanha o dedo sem deslocar o próprio arrasto.
 */
internal fun Modifier.swipeToActions(onSwipe: () -> Unit): Modifier = composed {
    val distance = with(LocalDensity.current) { SWIPE_DISTANCE.toPx() }
    val current by rememberUpdatedState(onSwipe)
    var dragX by remember { mutableFloatStateOf(0f) }
    pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { dragX = 0f },
            onDragCancel = { dragX = 0f },
            onDragEnd = {
                val opens = swipeOpensActions(dragX, distance)
                dragX = 0f
                if (opens) current()
            },
        ) { change, dx ->
            change.consume()
            dragX += dx
        }
    }.graphicsLayer { translationX = dragX * FOLLOW }
}
