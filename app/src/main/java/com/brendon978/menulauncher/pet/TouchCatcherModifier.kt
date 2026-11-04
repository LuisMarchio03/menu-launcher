package com.brendon978.menulauncher.pet

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.pointerInputCatcher(onDown: (Offset) -> Unit, onMove: (Offset) -> Unit, onUp: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown()
            d.consume()
            onDown(d.position)
            while (true) {
                val ch = awaitPointerEvent().changes.first()
                ch.consume()
                if (!ch.pressed) break
                onMove(ch.position)
            }
            onUp()
        }
    }
