package com.brendon978.menulauncher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.settings.BarSide
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Barra ★ A–Z # na borda direita (ou na esquerda, [side]), curva para dentro no meio (como no esboço).
 * Tocar ou arrastar seleciona; letra sem apps fica apagada.
 * Enquanto o dedo está na barra, um balão cresce para a esquerda mostrando a letra (o dedo cobre a barra).
 */
@Composable
fun AlphabetBar(
    selected: Char,
    available: Set<Char>,
    color: Color,
    onSelect: (Char) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    side: BarSide = BarSide.RIGHT,
    scale: Float = 1f,
) {
    val letters = LetterIndex.BAR
    val left = side == BarSide.LEFT
    val anchor = if (left) Alignment.BottomStart else Alignment.BottomEnd
    val haptic = LocalHapticFeedback.current
    val currentSelected = rememberUpdatedState(selected)
    val currentOnSelect = rememberUpdatedState(onSelect)

    BoxWithConstraints(modifier) {
        val itemHeight = min(15.5.dp * scale, maxHeight / letters.size)
        val density = LocalDensity.current
        val itemPx = with(density) { itemHeight.toPx() }
        // Altura de cada letra é fixa em dp: a fonte não pode crescer com a escala do sistema.
        val letterSize = with(density) { (itemHeight * 0.65f).toSp() }

        var pressed by remember { mutableStateOf(false) }
        var dragging by remember { mutableStateOf(false) }

        fun pick(y: Float) {
            val i = (y / itemPx).toInt().coerceIn(0, letters.lastIndex)
            if (letters[i] != currentSelected.value) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnSelect.value(letters[i])
            }
        }

        Column(
            horizontalAlignment = if (left) Alignment.Start else Alignment.End,
            modifier = Modifier
                .align(anchor)
                .pointerInput(itemPx) {
                    detectTapGestures(onPress = {
                        pick(it.y)
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    })
                }
                .pointerInput(itemPx) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true; pick(it.y) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ -> pick(change.position.y) }
                },
        ) {
            letters.forEachIndexed { i, letter ->
                val curve = curveOf(i, letters.size)
                val isSel = letter == selected
                val enabled = letter == LetterIndex.STAR || letter in available
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(itemHeight)
                        .padding(start = if (left) curve else 0.dp, end = if (left) 0.dp else curve)
                        .background(if (isSel) color else Color.Transparent, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp),
                ) {
                    Text(
                        letter.toString(),
                        fontSize = letterSize,
                        lineHeight = letterSize,
                        color = when {
                            isSel -> if (color.luminance() > 0.5f) Color.Black else Color.White
                            enabled -> color.copy(alpha = 0.8f)
                            else -> color.copy(alpha = 0.35f)
                        },
                    )
                }
            }
        }

        // Lupa na "barriga" da curva: o vão entre as letras do meio e a borda.
        SearchGlyph(
            color = color,
            onClick = onSearch,
            modifier = Modifier
                .align(anchor)
                .offset(y = -(itemHeight * (letters.size / 2f)) + 20.dp),
        )

        LetterBubble(
            letter = selected,
            visible = pressed || dragging,
            index = letters.indexOf(selected).coerceAtLeast(0),
            count = letters.size,
            itemHeight = itemHeight,
            color = color,
            left = left,
            modifier = Modifier.align(anchor),
        )
    }
}

/** Espaço que a barra ocupa na lateral: a lista não entra embaixo dela. */
fun barWidth(scale: Float): Dp = 52.dp * scale

/** Altura máxima da barra: metade da tela no tamanho original, sem nunca subir até o relógio. */
fun barMaxHeight(available: Dp, scale: Float): Dp = min(available / 2 * scale, available * 0.75f)

private fun curveOf(i: Int, count: Int) = (sin(i / (count - 1).toFloat() * PI) * 22).roundToInt().dp

/** Alvo de toque estreito (cabe no vão da curva) e alto, para o polegar acertar sem pegar as letras. */
@Composable
private fun SearchGlyph(color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lw = LocalLineScale.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 24.dp, height = 40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Buscar app",
                onClick = onClick,
            ),
    ) {
        Canvas(Modifier.size(16.dp)) {
            val w = size.width
            val stroke = w * 0.12f * lw
            drawCircle(color, radius = w * 0.32f, center = Offset(w * 0.42f, w * 0.42f), style = Stroke(stroke))
            drawLine(color, Offset(w * 0.66f, w * 0.66f), Offset(w * 0.92f, w * 0.92f), stroke, StrokeCap.Round)
        }
    }
}

/** Balão ao lado da letra escolhida, do lado de dentro da tela; cresce a partir da barra e some ao soltar. */
@Composable
private fun LetterBubble(
    letter: Char,
    visible: Boolean,
    index: Int,
    count: Int,
    itemHeight: Dp,
    color: Color,
    left: Boolean,
    modifier: Modifier = Modifier,
) {
    val size = 56.dp
    val scale by animateFloatAsState(if (visible) 1f else 0f, tween(140), label = "bubble-scale")
    // Centro vertical da letra, medido de baixo (a coluna está ancorada embaixo).
    val targetY = size / 2 - itemHeight * (count - index - 0.5f)
    val y by animateDpAsState(targetY, tween<Dp>(90), label = "bubble-y")
    // longe o bastante para o polegar não cobrir
    val x = (curveOf(index, count) + 52.dp).let { if (left) it else -it }
    if (scale == 0f) return
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(x = x, y = y)
            .requiredSize(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(if (left) 0f else 1f, 0.5f)
            }
            .background(color, CircleShape),
    ) {
        Text(
            letter.toString(),
            color = if (color.luminance() > 0.5f) Color.Black else Color.White,
            fontSize = with(LocalDensity.current) { 26.dp.toSp() },
        )
    }
}
