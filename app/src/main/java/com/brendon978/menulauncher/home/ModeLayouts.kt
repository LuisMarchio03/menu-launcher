package com.brendon978.menulauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.groupByCategory
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.pet.petObstacle
import com.brendon978.menulauncher.settings.BarSide
import com.brendon978.menulauncher.settings.LabelStyle
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.activeFavorites
import com.brendon978.menulauncher.settings.currentProfile
import com.brendon978.menulauncher.settings.sectionLabel
import kotlin.math.roundToInt

/** Um app como ícone com o nome pequeno embaixo (se os nomes estiverem ligados). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconCell(entry: AppEntry, settings: LauncherSettings, renderer: IconRenderer, size: Dp, onLaunch: (AppEntry) -> Unit, onLongPress: (AppEntry) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.combinedClickable(onClick = { onLaunch(entry) }, onLongClick = { onLongPress(entry) }),
    ) {
        AppIcon(entry, settings.iconOverrides[entry.key], settings.strokeColor, size, renderer, stroke = settings.strokeScale, shadow = settings.textShadow, tracksBadge = true)
        if (settings.showLabels) {
            Text(
                entry.label, color = Color(settings.strokeColor).copy(alpha = .85f), fontSize = settings.smallTextSize * .85f,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.width(size + 28.dp).padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun GroupTitle(text: String?, settings: LauncherSettings) {
    if (text == null) return
    val caps = settings.labelStyle == LabelStyle.CAPS
    Text(
        text,
        color = Color(settings.strokeColor).copy(alpha = settings.detailAlpha),
        fontSize = if (caps) settings.smallTextSize * .85f else settings.smallTextSize,
        letterSpacing = if (caps) 1.6.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
        modifier = Modifier.padding(top = 14.dp * settings.spacing, bottom = 8.dp),
    )
}

/** Todos os apps em gavetas automáticas (Social, Trabalho, Mídia...), abaixo do relógio. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CategoryMode(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    reservedTop: Dp,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(apps, settings.categoryOverrides) { groupByCategory(apps, settings.categoryOverrides) }
    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(top = reservedTop + 8.dp)
            .padding(horizontal = 22.dp)
            .petObstacle("list"),
    ) {
        items(groups, key = { it.first.name }) { (category, list) ->
            Column {
                GroupTitle(sectionLabel(category.label, settings.labelStyle), settings)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    list.forEach { IconCell(it, settings, renderer, settings.gridIconSize, onLaunch, onAppLongPress) }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 24.dp)) }
    }
}

/** Favoritos num leque no canto de baixo, do lado da barra de letras; o botão do canto abre todos os apps. */
@Composable
fun FanMode(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val favorites = settings.activeFavorites(currentProfile()).mapNotNull { byKey[it] }.take(FAN_INNER * 2)
    val right = settings.barSide == BarSide.RIGHT
    val icon = settings.gridIconSize * 1.25f
    val color = Color(settings.strokeColor)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val radius = with(density) { minOf(maxWidth * .62f, 210.dp).toPx() }
        val slots = remember(favorites.size, radius, right) { fanSlots(favorites.size, radius, right) }
        val corner = with(density) { androidx.compose.ui.geometry.Offset(if (right) maxWidth.toPx() - 36.dp.toPx() else 36.dp.toPx(), maxHeight.toPx() - 36.dp.toPx()) }
        val cell = icon + 28.dp
        val half = with(density) { cell.toPx() / 2 }
        favorites.forEachIndexed { i, entry ->
            val (dx, dy) = slots[i]
            Box(
                Modifier
                    .offset { IntOffset((corner.x + dx - half).roundToInt(), (corner.y + dy - half).roundToInt()) }
                    .size(cell)
                    .petObstacle("fan$i"),
                contentAlignment = Alignment.Center,
            ) { IconCell(entry, settings, renderer, icon, onLaunch, onAppLongPress) }
        }
        // o miolo do leque: todos os apps (a busca)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(if (right) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(12.dp)
                .size(48.dp)
                .border(LocalBorderWidth.current, color.copy(alpha = settings.detailAlpha), CircleShape)
                .clickable(onClick = onSearch)
                .petObstacle("list"),
        ) { Text("⋯", color = color, fontSize = 20.sp) }
    }
}

/** Até 5 favoritos numa barra translúcida embaixo; deslizar para cima (ou tocar na seta) abre todos os apps. */
@Composable
fun DockMode(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val dock = settings.activeFavorites(currentProfile()).mapNotNull { byKey[it] }.take(5)
    val color = Color(settings.strokeColor)
    Box(modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .petObstacle("list"),
        ) {
            Text(
                "⌃",
                color = color.copy(alpha = settings.detailAlpha),
                fontSize = 18.sp,
                modifier = Modifier.clickable(onClick = onSearch).padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = .08f), RoundedCornerShape(26.dp))
                    .border(LocalBorderWidth.current, color.copy(alpha = .14f), RoundedCornerShape(26.dp))
                    .padding(vertical = 12.dp),
            ) {
                if (dock.isEmpty()) {
                    Text("Segure um app na busca para favoritar", color = color.copy(alpha = .6f), fontSize = settings.smallTextSize)
                }
                dock.forEach { IconCell(it, settings, renderer, settings.gridIconSize * 1.15f, onLaunch, onAppLongPress) }
            }
        }
    }
}
