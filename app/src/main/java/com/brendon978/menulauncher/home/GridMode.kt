package com.brendon978.menulauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.LauncherSettings

/** Todos os apps em ordem alfabética; controles de música embaixo. Colunas e nomes vêm dos ajustes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridMode(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    musicSlotBottom: @Composable () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(apps) { LetterIndex.sorted(apps) }
    Column(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = if (settings.gridColumns > 0) GridCells.Fixed(settings.gridColumns)
            else GridCells.Adaptive(76.dp * settings.iconScale.coerceAtLeast(1f)),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        ) {
            items(sorted, key = { it.key.encode() }) { entry ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.combinedClickable(
                        onClick = { onLaunch(entry) },
                        onLongClick = { onAppLongPress(entry) },
                    ),
                ) {
                    AppIcon(
                        entry,
                        settings.iconOverrides[entry.key],
                        settings.strokeColor,
                        settings.gridIconSize,
                        renderer,
                        stroke = settings.strokeScale,
                        shadow = settings.textShadow,
                    )
                    if (settings.gridLabels) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            entry.label,
                            color = Color(settings.strokeColor),
                            fontSize = settings.gridTextSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        musicSlotBottom()
        // Na grade não há barra de letras: a busca fica fixa embaixo, na zona do polegar.
        SearchPillButton(
            color = Color(settings.strokeColor),
            onClick = onSearch,
            modifier = Modifier.padding(horizontal = 22.dp).padding(top = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}
