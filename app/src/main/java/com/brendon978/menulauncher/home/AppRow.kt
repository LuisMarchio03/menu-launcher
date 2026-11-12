package com.brendon978.menulauncher.home

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.remember
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
    val motion = rememberMotion(look.motion)
    val haptic = LocalHapticFeedback.current
    val end = look.align == com.brendon978.menulauncher.settings.ListAlign.END
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (look.align) {
            com.brendon978.menulauncher.settings.ListAlign.START -> androidx.compose.foundation.layout.Arrangement.Start
            com.brendon978.menulauncher.settings.ListAlign.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
            com.brendon978.menulauncher.settings.ListAlign.END -> androidx.compose.foundation.layout.Arrangement.End
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(look.height)
            .pressScale(press, motion, TransformOrigin(if (end) 1f else 0f, .5f), to = .95f)
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
        val icon = @Composable { AppIcon(entry, override, look.color, look.iconSize, renderer, stroke = look.stroke, shadow = look.shadow, tracksBadge = tracksBadge) }
        val name = @Composable {
            Text(
                entry.label, color = Color(look.color), fontSize = look.textSize, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (look.light) androidx.compose.ui.text.font.FontWeight.Light else null,
            )
        }
        // encostada à direita a linha espelha: nome e depois o ícone, colado na borda
        if (end) {
            if (look.label) { name(); Spacer(Modifier.width(14.dp)) }
            icon()
        } else {
            icon()
            if (look.label) { Spacer(Modifier.width(14.dp)); name() }
        }
    }
}
