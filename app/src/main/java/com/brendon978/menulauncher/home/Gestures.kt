package com.brendon978.menulauncher.home

import com.brendon978.menulauncher.settings.GestureAction
import com.brendon978.menulauncher.settings.LauncherSettings
import kotlin.math.abs

enum class Swipe { UP, DOWN }

/** Arrasto vertical numa área vazia: só conta se passar do limite (y cresce para baixo). */
fun swipeOf(totalDy: Float, thresholdPx: Float): Swipe? = when {
    abs(totalDy) < thresholdPx -> null
    totalDy > 0 -> Swipe.DOWN
    else -> Swipe.UP
}

fun LauncherSettings.actionFor(swipe: Swipe): GestureAction = when (swipe) {
    Swipe.UP -> swipeUp
    Swipe.DOWN -> swipeDown
}

/** Soma um arrasto vertical e, ao soltar, dispara [onSwipe] se passou do limite. */
class SwipeTracker(private val thresholdPx: Float, private val onSwipe: (Swipe) -> Unit) {
    private var total = 0f

    fun add(dy: Float) {
        total += dy
    }

    fun reset() {
        total = 0f
    }

    fun finish() {
        swipeOf(total, thresholdPx)?.let(onSwipe)
        total = 0f
    }
}
