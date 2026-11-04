package com.brendon978.menulauncher.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.brendon978.menulauncher.pet.Area
import com.brendon978.menulauncher.pet.LocalPetObstacles
import com.brendon978.menulauncher.pet.PetObstacles
import com.brendon978.menulauncher.pet.RobotPet
import com.brendon978.menulauncher.pet.petObstacle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.GestureAction
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings

@Composable
fun HomeScreen(
    homeOnTop: Boolean,
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    resetSignal: Int,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    onEmptyLongPress: () -> Unit,
    musicSlotTop: @Composable () -> Unit = {},
    musicSlotBottom: @Composable () -> Unit = {},
    onSearch: () -> Unit = {},
    musicPlaying: Boolean = false,
    /** Desenhado por cima de tudo (símbolo flutuante da IA). */
    overlay: @Composable () -> Unit = {},
    /** Gesto numa área vazia (arrastar para cima/baixo, toque duplo), com a ação escolhida nos ajustes. */
    onGesture: (GestureAction) -> Unit = {},
) {
    val color = Color(settings.strokeColor)
    val obstacles = remember { PetObstacles() }
    val current by rememberUpdatedState(settings)
    val gesture by rememberUpdatedState(onGesture)
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val swipe = remember(swipeThreshold) {
        SwipeTracker(swipeThreshold) { gesture(current.actionFor(it)) }
    }
    // Arrasto que começa numa lista (favoritos, grade): o que a lista não rolou (já no topo, ou sem
    // o que rolar) vale como gesto, decidido ao soltar o dedo.
    val listSwipe = remember(swipe) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) swipe.add(available.y)
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                swipe.finish()
                return Velocity.Zero
            }
        }
    }
    CompositionLocalProvider(LocalPetObstacles provides obstacles) {
    Box(
        Modifier
            .fillMaxSize()
            // só observa os toques (não consome): os olhos do Glitch seguem o seu dedo
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        ev.changes.firstOrNull()?.let {
                            com.brendon978.menulauncher.pet.GlitchSignals.lastTouch = Triple(it.position.x, it.position.y, System.currentTimeMillis())
                        }
                    }
                }
            }
            .pointerInput(settings.doubleTap == GestureAction.NONE) {
                detectTapGestures(
                    onLongPress = { onEmptyLongPress() },
                    // Sem ação no toque duplo, o detector nem espera o segundo toque.
                    onDoubleTap = if (current.doubleTap == GestureAction.NONE) null else ({ gesture(current.doubleTap) }),
                )
            }
            .nestedScroll(listSwipe)
            .pointerInput(swipe) {
                // Listas e a barra de letras consomem o próprio arrasto; aqui só chega o de área vazia.
                detectVerticalDragGestures(
                    onDragStart = { swipe.reset() },
                    onDragEnd = { swipe.finish() },
                    onDragCancel = { swipe.reset() },
                ) { _, dy -> swipe.add(dy) }
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (settings.mode) {
            HomeMode.ONE_HAND -> {
                Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp).petObstacle("top")) {
                    TopInfo(settings, color)
                    if (settings.showQuickActions) QuickActions(color)
                    musicSlotTop()
                }
                OneHandMode(
                    apps = apps,
                    settings = settings,
                    renderer = renderer,
                    resetSignal = resetSignal,
                    onLaunch = onLaunch,
                    onAppLongPress = onAppLongPress,
                    musicSlotBottom = musicSlotBottom,
                    onSearch = onSearch,
                )
            }
            // Grade: player ao lado do relógio; a grade começa logo abaixo do que for mais alto.
            HomeMode.GRID -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 12.dp)) {
                    Column {
                        TopInfo(settings, color)
                        if (settings.showQuickActions) QuickActions(color)
                    }
                    Box(Modifier.weight(1f).padding(start = 16.dp)) { musicSlotTop() }
                }
                GridMode(
                    apps = apps,
                    settings = settings,
                    renderer = renderer,
                    onLaunch = onLaunch,
                    onAppLongPress = onAppLongPress,
                    musicSlotBottom = musicSlotBottom,
                    onSearch = onSearch,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (settings.mode == HomeMode.ONE_HAND && settings.showPet) PetLayer(color, musicPlaying, settings, homeOnTop)
        overlay()
    }
    }
}

/** Camada do robozinho: a tela inteira, menos o que está marcado como obstáculo (com folga). */
@Composable
private fun PetLayer(color: Color, musicPlaying: Boolean, settings: LauncherSettings, homeOnTop: Boolean) {
    val reg = LocalPetObstacles.current ?: return
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot(); size = it.size },
    ) {
        if (size == IntSize.Zero) return@Box
        val gap = with(density) { 8.dp.toPx() }
        val margin = with(density) { 12.dp.toPx() }
        val obstacles = reg.rects.values.map {
            Area(it.left - origin.x - gap, it.top - origin.y - gap, it.right - origin.x + gap, it.bottom - origin.y + gap)
        }
        RobotPet(
            color = color,
            musicPlaying = musicPlaying,
            homeOnTop = homeOnTop,
            comments = settings.glitchComments,
            mischief = settings.glitchMischief,
            birthday = settings.birthday,
            city = settings.weatherCity,
            aiPackage = settings.aiPackage,
            area = Area(margin, margin, size.width - margin, size.height - margin),
            named = reg.rects.mapValues { (_, it) -> Area(it.left - origin.x, it.top - origin.y, it.right - origin.x, it.bottom - origin.y) },
            obstacles = obstacles,
            accessory = runCatching { com.brendon978.menulauncher.pet.Accessory.valueOf(settings.glitchAccessory) }.getOrDefault(com.brendon978.menulauncher.pet.Accessory.NONE),
        )
    }
}
