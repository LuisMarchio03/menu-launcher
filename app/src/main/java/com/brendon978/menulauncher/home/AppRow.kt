package com.brendon978.menulauncher.home

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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(look.height)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(if (onSwipe != null) Modifier.swipeToActions(onSwipe) else Modifier),
    ) {
        AppIcon(entry, override, look.color, look.iconSize, renderer, stroke = look.stroke, shadow = look.shadow, tracksBadge = tracksBadge)
        Spacer(Modifier.width(14.dp))
        Text(entry.label, color = Color(look.color), fontSize = look.textSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
