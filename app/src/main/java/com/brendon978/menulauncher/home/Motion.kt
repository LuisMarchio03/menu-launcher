package com.brendon978.menulauncher.home

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Curvas do Material 3: entrar desacelerando (emphasized decelerate) e sair acelerando. */
val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val ExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * Movimento ligado: o ajuste do launcher e as animações do sistema (Opções do desenvolvedor ou
 * "Remover animações" da acessibilidade zeram a escala de duração).
 */
@Composable
fun rememberMotion(enabled: Boolean): Boolean {
    val context = LocalContext.current
    val system = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) > 0f
    }
    return enabled && system
}

/**
 * Entrada em cascata de um item de lista: sobe 10dp e aparece, [index] passos depois do primeiro.
 * Só anima se o item nasce logo depois de [since] (a lista acabou de abrir); rolando, os itens chegam prontos.
 */
@Composable
fun Modifier.staggerIn(index: Int, since: Long, enabled: Boolean): Modifier {
    if (!enabled) return this
    val fresh = remember { SystemClock.uptimeMillis() - since < 350 }
    if (!fresh) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(28L * index.coerceAtMost(10))
        p.animateTo(1f, tween(280, easing = EnterEasing))
    }
    val lift = with(LocalDensity.current) { 10.dp.toPx() }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * lift
    }
}

/** Encolhe de leve enquanto o dedo está em cima e volta com uma mola curta. */
@Composable
fun Modifier.pressScale(source: InteractionSource, enabled: Boolean, origin: TransformOrigin = TransformOrigin.Center, to: Float = .94f): Modifier {
    if (!enabled) return this
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) to else 1f,
        if (pressed) tween(110, easing = EnterEasing) else spring(dampingRatio = .55f, stiffness = Spring.StiffnessMediumLow),
        label = "aperto",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = origin }
}

/**
 * Some aos poucos nas pontas de cima e de baixo, só do lado que ainda rola: a lista não termina
 * num corte seco no meio de um ícone.
 */
fun Modifier.fadingEdges(top: Boolean, bottom: Boolean, size: Dp = 28.dp): Modifier =
    if (!top && !bottom) this
    else graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val h = size.toPx().coerceAtMost(this.size.height / 3)
            if (top) drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = h), blendMode = BlendMode.DstIn)
            if (bottom) drawRect(
                Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = this.size.height - h, endY = this.size.height),
                blendMode = BlendMode.DstIn,
            )
        }
