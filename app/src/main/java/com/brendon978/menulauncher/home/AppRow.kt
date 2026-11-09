package com.brendon978.menulauncher.home

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.icons.IconRenderer

/** Ícone + nome, com tamanho, cor, traço e sombra de [look] (ver [rowLook]). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    entry: AppEntry,
    override: String?,
    look: RowLook,
    renderer: IconRenderer,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    tracksBadge: Boolean = false,
    /** Deslizar para o lado; null deixa a linha como sempre foi. */
    onSwipe: (() -> Unit)? = null,
) {
    // com movimento: a linha encolhe um pouco enquanto o dedo está nela e o toque vibra de leve
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (look.motion && pressed) .96f else 1f, label = "aperto")
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(look.height)
            .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, .5f) }
            .combinedClickable(
                interactionSource = press,
                indication = null,
                onClick = {
                    if (look.motion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick,
            )
            .then(if (onSwipe != null) Modifier.swipeToActions(onSwipe) else Modifier),
    ) {
        AppIcon(entry, override, look.color, look.iconSize, renderer, stroke = look.stroke, shadow = look.shadow, tracksBadge = tracksBadge)
        Spacer(Modifier.width(14.dp))
        Text(
            entry.label, color = Color(look.color), fontSize = look.textSize, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (look.light) androidx.compose.ui.text.font.FontWeight.Light else null,
        )
    }
}
