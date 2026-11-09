package com.brendon978.menulauncher.home

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import com.brendon978.menulauncher.settings.ListAlign
import com.brendon978.menulauncher.settings.BlockPosition
import com.brendon978.menulauncher.settings.LabelStyle
import com.brendon978.menulauncher.settings.sectionLabel
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.pet.petObstacle
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.BarSide
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.activeFavorites

/**
 * Metade de baixo: favoritos (ou os apps da letra escolhida) + controles de música; barra de letras na borda
 * escolhida nos ajustes (direita por padrão).
 * [resetSignal] muda quando o usuário volta para a tela inicial: a seleção volta para ★.
 */
@Composable
fun OneHandMode(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    strokeColor: Int,
    renderer: IconRenderer,
    resetSignal: Int,
    /** Altura ocupada no topo (0 = não limita a lista além da metade da tela). */
    reservedTop: Dp = 0.dp,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    musicSlotBottom: @Composable () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(LetterIndex.STAR) }
    LaunchedEffect(resetSignal) { selected = LetterIndex.STAR }

    val groups = remember(apps) { LetterIndex.group(apps) }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    // favoritos por horário: relê a cada minuto, enquanto a opção estiver ligada
    val profile by androidx.compose.runtime.produceState(com.brendon978.menulauncher.settings.currentProfile()) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            value = com.brendon978.menulauncher.settings.currentProfile()
        }
    }
    val activeFavorites = settings.activeFavorites(profile)
    val favorites = remember(activeFavorites, byKey) { activeFavorites.mapNotNull { byKey[it] } }
    // travessura do Glitch: empurra o primeiro favorito até você cutucar ele
    val pushed by androidx.compose.animation.core.animateDpAsState(
        if (com.brendon978.menulauncher.pet.GlitchMischief.current.value == com.brendon978.menulauncher.pet.Mischief.ICON) 46.dp else 0.dp,
        label = "empurrao",
    )
    val color = Color(strokeColor)

    val shown = if (selected == LetterIndex.STAR) favorites else groups[selected].orEmpty()
    val header = when {
        selected == LetterIndex.STAR -> "Favoritos"
        shown.isEmpty() -> "$selected · nenhum app"
        else -> selected.toString()
    }

    val left = settings.barSide == BarSide.LEFT
    val look = settings.rowLook
    BoxWithConstraints(modifier.fillMaxSize()) {
        val half = maxHeight / 2
        val barRoom = barWidth(settings.barScale)
        val listMax = if (reservedTop > 0.dp) minOf(half, (maxHeight - reservedTop).coerceAtLeast(MIN_LIST)) else half
        // bloco embaixo (o original), no meio ou logo abaixo do relógio; lista encostada no lado escolhido
        val block = settings.blockPosition
        Column(
            verticalArrangement = if (block == BlockPosition.TOP) Arrangement.Top else Arrangement.Bottom,
            horizontalAlignment = when (settings.listAlign) {
                ListAlign.START -> Alignment.Start
                ListAlign.CENTER -> Alignment.CenterHorizontally
                ListAlign.END -> Alignment.End
            },
            modifier = Modifier
                .align(
                    when (block) {
                        BlockPosition.BOTTOM -> Alignment.BottomStart
                        BlockPosition.MIDDLE -> Alignment.CenterStart
                        BlockPosition.TOP -> Alignment.TopStart
                    },
                )
                .padding(top = if (block == BlockPosition.TOP) reservedTop + 16.dp else 0.dp)
                .fillMaxWidth()
                .heightIn(max = listMax)
                .padding(start = if (left) barRoom else 22.dp, end = if (left) 22.dp else barRoom)
                .petObstacle("list"),
        ) {
            if (settings.showMostUsed && selected == LetterIndex.STAR) {
                val most = remember(settings.usage, activeFavorites, settings.hiddenApps, byKey) {
                    com.brendon978.menulauncher.settings.Usage.top(settings.usage, 5, activeFavorites.toSet() + settings.hiddenApps).mapNotNull { byKey[it] }
                }
                if (most.isNotEmpty()) {
                    SectionTitle(sectionLabel("Mais usados", settings.labelStyle), color, settings)
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.padding(bottom = 14.dp * settings.spacing),
                    ) {
                        most.forEach { entry ->
                            androidx.compose.foundation.layout.Box(
                                Modifier.combinedClickableCompat(onClick = { onLaunch(entry) }, onLongClick = { onAppLongPress(entry) }),
                            ) { AppIcon(entry, settings.iconOverrides[entry.key], settings.strokeColor, settings.gridIconSize, renderer, stroke = settings.strokeScale, shadow = settings.textShadow) }
                        }
                    }
                }
            }
            // letra sem app avisa sempre; o resto segue o estilo dos títulos
            SectionTitle(if (shown.isEmpty()) header else sectionLabel(header, settings.labelStyle), color, settings)
            val pages = if (settings.favoritePages && selected == LetterIndex.STAR) remember(favorites, settings.folders, byKey) {
                favoritePages(favorites, settings.folders, byKey)
            } else null
            if (pages != null && pages.size > 1) {
                // favoritos e cada pasta lado a lado: arrastar para o lado troca de página
                val pager = androidx.compose.foundation.pager.rememberPagerState { pages.size }
                androidx.compose.foundation.pager.HorizontalPager(pager, modifier = Modifier.weight(1f, fill = false), verticalAlignment = Alignment.Bottom) { i ->
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.Bottom) {
                        if (i > 0) SectionTitle(sectionLabel(pages[i].first, settings.labelStyle), color, settings)
                        pages[i].second.forEach { entry ->
                            AppRow(entry, settings.iconOverrides[entry.key], look, renderer, { onLaunch(entry) }, { onAppLongPress(entry) })
                        }
                    }
                }
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    repeat(pages.size) { i ->
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(6.dp).background(color.copy(alpha = if (pager.currentPage == i) .9f else .3f), androidx.compose.foundation.shape.CircleShape),
                        )
                    }
                }
            } else LazyColumn(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(shown, key = { it.key.encode() }) { entry ->
                    val first = selected == LetterIndex.STAR && entry === shown.first()
                    Box(Modifier.then(if (first) Modifier.offset(x = pushed) else Modifier)) {
                        AppRow(
                            entry = entry,
                            override = settings.iconOverrides[entry.key],
                            look = look,
                            renderer = renderer,
                            onClick = { onLaunch(entry) },
                            onLongClick = { onAppLongPress(entry) },
                            tracksBadge = true,
                            onSwipe = if (settings.swipeAppActions) ({ onAppLongPress(entry) }) else null,
                        )
                    }
                }
            }
            musicSlotBottom()
            Box(Modifier.height(8.dp))
        }
        AlphabetBar(
            selected = selected,
            available = groups.keys,
            color = color,
            onSelect = { selected = it },
            onSearch = onSearch,
            side = settings.barSide,
            scale = settings.barScale,
            curved = settings.barCurved,
            dim = (settings.detailAlpha + 0.2f).coerceAtMost(1f),
            modifier = Modifier
                .align(if (left) Alignment.BottomStart else Alignment.BottomEnd)
                .heightIn(max = barMaxHeight(maxHeight, settings.barScale))
                .padding(start = if (left) 4.dp else 0.dp, end = if (left) 0.dp else 4.dp, bottom = 8.dp)
                .petObstacle("bar"),
        )
    }
}

/** Título de seção pequeno e apagado; em maiúsculas ganha espaço entre as letras. */
@Composable
private fun SectionTitle(text: String?, color: Color, settings: LauncherSettings) {
    if (text == null) return
    val caps = settings.labelStyle == LabelStyle.CAPS
    Text(
        text,
        color = color.copy(alpha = settings.detailAlpha),
        fontSize = if (caps) settings.smallTextSize * 0.85f else settings.smallTextSize,
        letterSpacing = if (caps) 1.6.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
        modifier = Modifier.padding(bottom = 6.dp * settings.spacing),
    )
}

/** Pelo menos duas linhas e o título: a lista nunca some, mesmo com o topo cheio. */
private val MIN_LIST = 110.dp

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.then(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick))
