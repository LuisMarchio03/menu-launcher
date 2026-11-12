package com.brendon978.menulauncher.home

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.brendon978.menulauncher.pet.Area
import com.brendon978.menulauncher.pet.LocalPetObstacles
import com.brendon978.menulauncher.pet.PetObstacles
import com.brendon978.menulauncher.pet.RobotPet
import com.brendon978.menulauncher.pet.petObstacle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
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
import com.brendon978.menulauncher.settings.ClockAlign
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.listensDoubleTap

@Composable
fun HomeScreen(
    homeOnTop: Boolean,
    apps: List<AppEntry>,
    settings: LauncherSettings,
    /** A cor efetiva do traço (ver rememberStrokeColor). */
    strokeColor: Int,
    renderer: IconRenderer,
    resetSignal: Int,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    onEmptyLongPress: () -> Unit,
    musicSlotTop: @Composable () -> Unit = {},
    musicSlotBottom: @Composable () -> Unit = {},
    onSearch: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onEmptyDoubleTap: () -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    /** Texto do tempo de tela; null não mostra nada. */
    screenTime: String? = null,
    musicPlaying: Boolean = false,
    /** Desenhado por cima de tudo (símbolo flutuante da IA). */
    overlay: @Composable () -> Unit = {},
) {
    val color = Color(strokeColor)
    val obstacles = remember { PetObstacles() }
    // os gestos ficam escutando por muito tempo: sem isto rodaria a ação escolhida antes da troca nos Ajustes
    val swipeDown by rememberUpdatedState(onSwipeDown)
    val swipeUp by rememberUpdatedState(onSwipeUp)
    val doubleTap by rememberUpdatedState(onEmptyDoubleTap)
    val dockMode by rememberUpdatedState(settings.mode == HomeMode.DOCK)
    val search by rememberUpdatedState(onSearch)
    val listensDoubleTap = settings.listensDoubleTap()
    // o bloco de apps no meio ou em cima precisa saber onde o topo acaba, e os modos novos começam logo abaixo dele
    val growsTop = settings.showNote || settings.showContacts || screenTime != null ||
        settings.blockPosition != com.brendon978.menulauncher.settings.BlockPosition.BOTTOM ||
        settings.mode == HomeMode.CATEGORIES
    var topHeight by remember { mutableIntStateOf(0) }
    // atalhos que só aparecem ao tocar na linha da data: somem sozinhos depois de um tempo
    var quickShown by remember { mutableStateOf(false) }
    LaunchedEffect(quickShown) { if (quickShown) { kotlinx.coroutines.delay(8_000); quickShown = false } }
    val showQuick = settings.showQuickActions && (settings.quickReveal == com.brendon978.menulauncher.settings.QuickReveal.ALWAYS || quickShown)
    val onInfoTap: (() -> Unit)? =
        if (settings.showQuickActions && settings.quickReveal == com.brendon978.menulauncher.settings.QuickReveal.ON_TAP) ({ quickShown = !quickShown }) else null
    // cada volta para a tela inicial (ou toque no home) refaz a entrada em sequência
    val entrance = remember(homeOnTop, resetSignal) { if (homeOnTop) resetSignal + 1 else 0 }
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val swipe = remember(swipeThreshold) {
        SwipeTracker(swipeThreshold) {
            when {
                it == Swipe.DOWN -> swipeDown()
                // na barra fixa, deslizar para cima é sempre a gaveta com todos os apps
                dockMode -> search()
                else -> swipeUp()
            }
        }
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
            .pointerInput(listensDoubleTap) {
                detectTapGestures(
                    onLongPress = { onEmptyLongPress() },
                    // só escuta toque duplo com a opção ligada (senão o toque simples nem espera)
                    onDoubleTap = if (listensDoubleTap) ({ doubleTap() }) else null,
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
            // véu nas pontas: o relógio e a lista leem sobre papel de parede com muito detalhe
            .then(if (settings.edgeVeil > 0f) Modifier.edgeVeil(settings.edgeVeil) else Modifier)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (settings.mode) {
            HomeMode.ONE_HAND, HomeMode.CATEGORIES, HomeMode.FAN, HomeMode.DOCK -> {
                // alinhado ao centro ou à direita, o topo ocupa a largura toda
                val align = settings.clockAlign
                Column(
                    Modifier
                        // com o que é novo ligado a coluna cresce: a lista respeita o que sobrar
                        .then(if (growsTop) Modifier.onSizeChanged { topHeight = it.height } else Modifier)
                        .then(if (align != ClockAlign.START) Modifier.fillMaxWidth() else Modifier)
                        .padding(start = 22.dp, end = 22.dp, top = 24.dp * settings.spacing)
                        .enterIn(0, entrance, settings.motion)
                        .petObstacle("top"),
                    horizontalAlignment = when (align) {
                        ClockAlign.START -> Alignment.Start
                        ClockAlign.CENTER -> Alignment.CenterHorizontally
                        ClockAlign.END -> Alignment.End
                    },
                ) {
                    TopInfo(settings, color, screenTime = screenTime, align = align, onInfoTap = onInfoTap)
                    androidx.compose.animation.AnimatedVisibility(showQuick) { QuickActions(color, settings) }
                    TopExtras(settings, color, onNoteChange)
                    musicSlotTop()
                }
                val reserved = if (growsTop) with(LocalDensity.current) { topHeight.toDp() } else 0.dp
                val below = Modifier.enterIn(1, entrance, settings.motion)
                when (settings.mode) {
                    HomeMode.CATEGORIES -> CategoryMode(apps, settings, renderer, reserved, onLaunch, onAppLongPress, below)
                    HomeMode.FAN -> FanMode(apps, settings, renderer, onLaunch, onAppLongPress, onSearch, below)
                    HomeMode.DOCK -> DockMode(apps, settings, renderer, onLaunch, onAppLongPress, onSearch, below)
                    else -> OneHandMode(
                        apps = apps,
                        settings = settings,
                        strokeColor = strokeColor,
                        renderer = renderer,
                        resetSignal = resetSignal,
                        reservedTop = reserved,
                        onLaunch = onLaunch,
                        onAppLongPress = onAppLongPress,
                        musicSlotBottom = musicSlotBottom,
                        onSearch = onSearch,
                        modifier = below,
                    )
                }
            }
            // Grade: player ao lado do relógio; a grade começa logo abaixo do que for mais alto.
            HomeMode.GRID -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp * settings.spacing, bottom = 12.dp * settings.spacing)) {
                    Column {
                        TopInfo(settings, color, screenTime = screenTime, onInfoTap = onInfoTap)
                        androidx.compose.animation.AnimatedVisibility(showQuick) { QuickActions(color, settings) }
                        TopExtras(settings, color, onNoteChange)
                    }
                    Box(Modifier.weight(1f).padding(start = 16.dp)) { musicSlotTop() }
                }
                GridMode(
                    apps = apps,
                    settings = settings,
                    strokeColor = strokeColor,
                    renderer = renderer,
                    onLaunch = onLaunch,
                    onAppLongPress = onAppLongPress,
                    musicSlotBottom = musicSlotBottom,
                    onSearch = onSearch,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // grade e categorias ocupam a tela toda com ícones: o Glitch não teria onde ficar
        if (settings.mode != HomeMode.GRID && settings.mode != HomeMode.CATEGORIES && settings.showPet) PetLayer(color, musicPlaying, settings, homeOnTop)
        overlay()
    }
    }
}

/** O que entra no topo só quando ligado nos Ajustes. */
@Composable
private fun TopExtras(settings: LauncherSettings, color: Color, onNoteChange: (String) -> Unit) {
    if (settings.showNote) QuickNote(settings.noteText, color, onNoteChange, settings)
    if (settings.showContacts && settings.contactShortcuts.isNotEmpty()) ContactShortcutsRow(settings.contactShortcuts, color)
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
            buddy = runCatching { com.brendon978.menulauncher.pet.Buddy.valueOf(settings.glitchBuddy) }.getOrDefault(com.brendon978.menulauncher.pet.Buddy.NONE),
            aiPackage = settings.aiPackage,
            area = Area(margin, margin, size.width - margin, size.height - margin),
            named = reg.rects.mapValues { (_, it) -> Area(it.left - origin.x, it.top - origin.y, it.right - origin.x, it.bottom - origin.y) },
            obstacles = obstacles,
            accessory = runCatching { com.brendon978.menulauncher.pet.Accessory.valueOf(settings.glitchAccessory) }.getOrDefault(com.brendon978.menulauncher.pet.Accessory.NONE),
            calmGarden = settings.glitchCalm,
        )
    }
}

/** Degradê escuro no topo e no pé da tela, com a força [strength] (0 a 1). */
private fun Modifier.edgeVeil(strength: Float): Modifier = drawBehind {
    val top = Color.Black.copy(alpha = .55f * strength)
    drawRect(Brush.verticalGradient(listOf(top, Color.Transparent), endY = size.height * .32f), size = size.copy(height = size.height * .32f))
    val from = size.height * .55f
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .7f * strength)), startY = from, endY = size.height),
        topLeft = Offset(0f, from),
        size = size.copy(height = size.height - from),
    )
}

/**
 * Entrada em sequência: o bloco [index] sobe 12dp e aparece, um pouco depois do anterior.
 * [trigger] 0 = parado (outra tela por cima); cada valor novo refaz a entrada.
 */
@Composable
private fun Modifier.enterIn(index: Int, trigger: Int, enabled: Boolean): Modifier {
    if (!rememberMotion(enabled)) return this
    val p = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        p.snapTo(0f)
        kotlinx.coroutines.delay(70L * index)
        p.animateTo(1f, androidx.compose.animation.core.tween(340, easing = EnterEasing))
    }
    val lift = with(LocalDensity.current) { 14.dp.toPx() }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * lift
        // chega quase no tamanho, sem "pulo": só um respiro de 2%
        val s = .98f + .02f * p.value
        scaleX = s
        scaleY = s
    }
}
