package com.brendon978.menulauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.brendon978.menulauncher.settings.FolderOps
import com.brendon978.menulauncher.settings.GridItem
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
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.LauncherSettings

/** Todos os apps em ordem alfabética; controles de música embaixo. Colunas e nomes vêm dos ajustes. */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GridMode(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    strokeColor: Int,
    renderer: IconRenderer,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    musicSlotBottom: @Composable () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(apps, settings.folders) { FolderOps.gridItems(apps, settings.folders) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    val color = Color(strokeColor)
    Column(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = if (settings.gridColumns > 0) GridCells.Fixed(settings.gridColumns)
            else GridCells.Adaptive(76.dp * settings.iconScale.coerceAtLeast(1f)),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        ) {
            items(items, key = { if (it is GridItem.App) it.entry.key.encode() else "pasta:" + it.label }) { item ->
                when (item) {
                    is GridItem.App -> GridCell(item.entry.label, settings, onClick = { onLaunch(item.entry) }, onLongClick = { onAppLongPress(item.entry) }) {
                        GridIcon(item.entry, settings, renderer)
                    }
                    is GridItem.Folder -> GridCell(item.name, settings, onClick = { openFolder = item.name }, onLongClick = { openFolder = item.name }) {
                        // pasta: moldura com até 4 mini ícones
                        Box(Modifier.size(settings.gridCellSize).border(LocalBorderWidth.current * 1.4f, color, RoundedCornerShape(8.dp)).padding(3.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                item.apps.take(4).chunked(2).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        row.forEach { AppIcon(it, settings.iconOverrides[it.key], settings.strokeColor, 11.dp * settings.iconScale, renderer, stroke = settings.strokeScale) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        musicSlotBottom()
        // Na grade não há barra de letras: a busca fica fixa embaixo, na zona do polegar.
        SearchPillButton(
            color = color,
            onClick = onSearch,
            modifier = Modifier.padding(horizontal = 22.dp).padding(top = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }

    val folder = items.filterIsInstance<GridItem.Folder>().firstOrNull { it.name == openFolder }
    if (openFolder != null && folder == null) openFolder = null
    folder?.let { f ->
        ModalBottomSheet(
            onDismissRequest = { openFolder = null },
            containerColor = Color(0xFF18181B),
        ) {
            Text(f.name, color = color, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp * settings.iconScale.coerceAtLeast(1f)),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.padding(16.dp).navigationBarsPadding(),
            ) {
                items(f.apps, key = { it.key.encode() }) { entry ->
                    GridCell(entry.label, settings, onClick = { openFolder = null; onLaunch(entry) }, onLongClick = { openFolder = null; onAppLongPress(entry) }) {
                        GridIcon(entry, settings, renderer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCell(label: String, settings: LauncherSettings, onClick: () -> Unit, onLongClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(Modifier.size(settings.gridCellSize), contentAlignment = Alignment.Center) { icon() }
        if (settings.gridLabels) {
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                color = Color(settings.strokeColor),
                fontSize = settings.gridTextSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GridIcon(entry: AppEntry, settings: LauncherSettings, renderer: IconRenderer) {
    AppIcon(
        entry,
        settings.iconOverrides[entry.key],
        settings.strokeColor,
        settings.gridIconSize,
        renderer,
        stroke = settings.strokeScale,
        shadow = settings.textShadow,
    )
}
