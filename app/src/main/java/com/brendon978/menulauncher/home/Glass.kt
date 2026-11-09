package com.brendon978.menulauncher.home

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.util.function.Consumer

/** true = a tela de cima está sobre o papel de parede desfocado (vidro); false = fundo escuro cheio. */
val LocalGlass = compositionLocalOf { false }

private val DARK = Color(0xFF0C0C0E)

/** Fundo das telas que abrem por cima (Ajustes, busca, ícone): véu translúcido no vidro, escuro cheio sem. */
fun overlayBackground(glass: Boolean, solidAlpha: Float = 1f): Color = if (glass) DARK.copy(alpha = .62f) else DARK.copy(alpha = solidAlpha)

/** Cartões e blocos em cima desse fundo. */
fun cardFill(glass: Boolean): Color = if (glass) Color.White.copy(alpha = .08f) else Color(0xFF17171C)

/** Miniaturas e opções com desenho (relógios, modos). */
fun tileFill(glass: Boolean): Color = if (glass) Color.Black.copy(alpha = .28f) else Color(0xFF0F0F13)

/**
 * Desfoca o papel de parede atrás da janela enquanto [active]. Devolve se o desfoque está valendo:
 * o sistema desliga o desfoque entre janelas na economia de bateria, e aí a tela volta ao escuro cheio.
 */
@Composable
fun rememberBlurBehind(activity: Activity, active: Boolean, radius: Int = 80): Boolean {
    val wm = activity.windowManager
    var supported by remember { mutableStateOf(wm.isCrossWindowBlurEnabled) }
    DisposableEffect(wm) {
        val listener = Consumer<Boolean> { supported = it }
        wm.addCrossWindowBlurEnabledListener(listener)
        onDispose { wm.removeCrossWindowBlurEnabledListener(listener) }
    }
    val on = active && supported
    DisposableEffect(on) {
        val window = activity.window
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = radius }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
        onDispose { }
    }
    return on
}
