package com.brendon978.menulauncher.settingsui

import androidx.compose.runtime.LaunchedEffect
import com.brendon978.menulauncher.settings.applyTheme
import com.brendon978.menulauncher.settings.LookTheme
import com.brendon978.menulauncher.home.LocalGlass
import com.brendon978.menulauncher.home.cardFill
import com.brendon978.menulauncher.home.overlayBackground
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.home.LocalBorderWidth
import com.brendon978.menulauncher.settings.Backup
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.MusicStyle
import com.brendon978.menulauncher.icons.IconRenderer
import androidx.compose.runtime.remember
import com.brendon978.menulauncher.settings.activeFavorites
import com.brendon978.menulauncher.settings.editFavorites
import kotlinx.coroutines.launch

/** Toque longo numa área vazia abre aqui. Tudo se ajusta no celular. */
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    apps: Map<AppKey, AppEntry>,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    hasUsageAccess: Boolean,
    onSetDefaultLauncher: () -> Unit,
    aiSection: @Composable () -> Unit,
    onClose: () -> Unit,
    /** Os ajustes como a tela inicial os usa agora (cor automática já resolvida), para a prévia. */
    preview: LauncherSettings = settings,
    renderer: IconRenderer? = null,
) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var undoTheme by remember { mutableStateOf<LookTheme?>(null) }
    LaunchedEffect(undoTheme) { if (undoTheme != null) { kotlinx.coroutines.delay(5_000); undoTheme = null } }
    val glass = LocalGlass.current
    BackHandler(enabled = page != null) { page = null }
    // Cada página começa do topo.
    val scroll = key(page) { rememberScrollState() }
    // onde a prévia está, em coordenadas da janela; o fundo escuro é pintado em volta dela
    var hole by remember { mutableStateOf<Rect?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Column(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .drawBehind {
                val bg = overlayBackground(glass)
                val h = hole
                if (h == null) drawRect(bg) else {
                    val r = h.translate(-origin)
                    val path = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRoundRect(RoundRect(r, CornerRadius(22.dp.toPx())))
                    }
                    drawPath(path, bg)
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            if (page != null) {
                Text("‹", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { page = null }.padding(end = 14.dp, bottom = 4.dp))
            }
            Text(page?.title ?: "Ajustes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Fechar", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
        // Prévia fixa no topo onde a escolha muda a cara da tela inicial; o resto rola embaixo dela.
        // buscando, a prévia sai e os resultados ficam acima do teclado
        if (renderer != null && query.isBlank() && (page == null || page == SettingsPage.LOOK || page == SettingsPage.HOME)) {
            val shownApps = remember(settings.favorites, apps) {
                settings.favorites.mapNotNull { apps[it] }.ifEmpty { apps.values.sortedBy { it.label.lowercase() } }.take(4)
            }
            LivePreview(
                preview, shownApps, renderer,
                Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp).onGloballyPositioned { hole = it.boundsInWindow() },
            )
            DisposableEffect(Unit) { onDispose { hole = null } }
        }
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            when (page) {
                null -> {
                    SettingsSearch(query, { query = it }) { hit -> query = ""; page = SettingsPage.valueOf(hit.page) }
                    if (query.isBlank()) {
                    GroupLabel("Temas")
                    ThemesRow(settings, preview.strokeColor, onChange) { before -> undoTheme = before }
                    GroupLabel("Ajustar")
                    SettingsPage.entries.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                            pair.forEach { p -> CategoryTile(p.title, p.hint, p.glyph, p.accent, { page = p }, Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    }
                }
                SettingsPage.LOOK -> LookPage(settings, onChange)
                SettingsPage.HOME -> HomePage(settings, apps, onChange, hasUsageAccess)
                SettingsPage.APPS -> AppsPage(settings, apps, onChange)
                SettingsPage.GLITCH -> GlitchPage(settings, onChange)
                SettingsPage.AI -> SettingsCard { aiSection() }
                SettingsPage.WALLPAPER -> {
                    SettingsCard { WallpaperSection() }
                    SettingsCard { WallpaperRotationSection(settings, onChange) }
                }
                SettingsPage.SYSTEM -> SystemPage(settings, onChange, onSetDefaultLauncher)
            }
            Spacer(Modifier.height(24.dp))
        }
        // aplicou um tema sem querer: 5 segundos para voltar ao que era
        undoTheme?.let { before ->
            UndoBar("Tema aplicado", "Desfazer") {
                undoTheme = null
                onChange { it.applyTheme(before) }
            }
        }
    }
}

/** As páginas dos Ajustes, na ordem da grade da tela principal. */
private enum class SettingsPage(val title: String, val hint: String, val glyph: String, val accent: Color) {
    LOOK("Aparência", "Relógio, cor, fonte, tamanhos", "◐", Color(0xFF9D8CFF)),
    HOME("Tela inicial", "Widgets, música e gestos", "⌂", Color(0xFF4CD18A)),
    APPS("Apps", "Favoritos e escondidos", "▦", Color(0xFFFFA94D)),
    GLITCH("Glitch", "Acessório, bichinho, humor", "☻", Color(0xFFFF7BA5)),
    AI("IA", "Assistente e símbolo", "✦", Color(0xFF5CB8FF)),
    WALLPAPER("Papel de parede", "Aplicar e trocar sozinho", "▨", Color(0xFF38D9C0)),
    SYSTEM("Backup e sistema", "Exportar e tela padrão", "⚙", Color(0xFFADB5BD)),
}

private val CARD_RADIUS = 18.dp

/** Bloco de uma página dos Ajustes. */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .background(cardFill(LocalGlass.current), RoundedCornerShape(CARD_RADIUS))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun LookPage(settings: LauncherSettings, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    SettingsCard {
        Section("Modo")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PictureChoice("Uma mão", settings.mode == HomeMode.ONE_HAND, { onChange { it.copy(mode = HomeMode.ONE_HAND) } }, Modifier.weight(1f)) {
                Bars(54, 40, 48, 36)
            }
            PictureChoice("Grade", settings.mode == HomeMode.GRID, { onChange { it.copy(mode = HomeMode.GRID) } }, Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(4) { Box(Modifier.size(12.dp).border(1.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(4.dp))) }
                        }
                    }
                }
            }
        }

    }
    SettingsCard { SizeSection(settings, onChange) }
    SettingsCard { ClockSection(settings, onChange) }
    SettingsCard { FinishSection(settings, onChange) }
    SettingsCard {
        Section("Cor do traço")
        ColorRow(settings.strokeColor) { c -> onChange { it.copy(strokeColor = c) } }
        var hex by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = hex,
            onValueChange = { v ->
                hex = v
                parseHexColor(v)?.let { c -> onChange { it.copy(strokeColor = c) } }
            },
            singleLine = true,
            placeholder = { Text("ou digite um hex, ex.: #A0C4FF") },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Text("Cor automática", color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            com.brendon978.menulauncher.settings.StrokeAuto.entries.forEach { m ->
                Chip(m.label, settings.strokeAuto == m) { onChange { it.copy(strokeAuto = m) } }
            }
        }
        if (settings.strokeAuto == com.brendon978.menulauncher.settings.StrokeAuto.BY_TIME) {
            Text("Dia (7h às 19h)", color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            ColorRow(settings.strokeDay) { c -> onChange { it.copy(strokeDay = c) } }
            Text("Noite", color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            ColorRow(settings.strokeNight) { c -> onChange { it.copy(strokeNight = c) } }
        }
        if (settings.strokeAuto == com.brendon978.menulauncher.settings.StrokeAuto.BY_WALLPAPER) {
            Text(
                "Usa a cor mais viva do papel de parede, ajustada para dar contraste, e liga a sombra no tom oposto para ler sobre qualquer parte dele. Se ele não informar cor, vale a cor escolhida acima.",
                color = Color.White.copy(alpha = .5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        WallpaperPalette { c -> onChange { it.copy(strokeColor = c, strokeAuto = com.brendon978.menulauncher.settings.StrokeAuto.OFF) } }

    }
    SettingsCard { IconLookSection(settings, onChange) }
    SettingsCard { FontSection(settings, onChange) }
    SettingsCard { BarSideSection(settings, onChange) }
    SettingsCard { GridSection(settings, onChange) }
}

@Composable
private fun HomePage(
    settings: LauncherSettings,
    apps: Map<AppKey, AppEntry>,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    hasUsageAccess: Boolean,
) {
    SettingsCard {
        Section("Tela inicial")
        ToggleRow("Linha de mais usados", "No modo uma mão, os apps que você mais abre, em cima dos favoritos.", settings.showMostUsed) { v -> onChange { it.copy(showMostUsed = v) } }
        ToggleRow("Deslizar um app para o lado", "Na lista da tela inicial, abre as mesmas opções de segurar o app.", settings.swipeAppActions) { v -> onChange { it.copy(swipeAppActions = v) } }
        ToggleRow("Atalhos rápidos", "Lanterna, Wi-Fi, internet e não perturbe embaixo da data.", settings.showQuickActions) { v -> onChange { it.copy(showQuickActions = v) } }
        ToggleRow("Bloco de notas", "Uma nota curta na tela inicial; tocar edita.", settings.showNote) { v -> onChange { it.copy(showNote = v) } }
        ScreenTimeToggle(settings.showScreenTime, hasUsageAccess) { v -> onChange { it.copy(showScreenTime = v) } }
        ContactsSection(settings, onChange)
        NextEventToggle(settings.showNextEvent) { v -> onChange { it.copy(showNextEvent = v) } }
        ContactsToggle(settings.searchContacts) { v -> onChange { it.copy(searchContacts = v) } }

    }
    SettingsCard {
        Section("Música")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar o que está tocando", color = Color.White, modifier = Modifier.weight(1f))
            Switch(settings.showMusic, onCheckedChange = { v -> onChange { it.copy(showMusic = v) } })
        }
        if (settings.showMusic) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                PictureChoice("Faixa fina", settings.musicStyle == MusicStyle.CLASSIC, { onChange { it.copy(musicStyle = MusicStyle.CLASSIC) } }, Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(14.dp).border(1.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(3.dp)))
                        Bars(50, 34, height = 4)
                    }
                }
                PictureChoice("Com capa", settings.musicStyle == MusicStyle.COVER, { onChange { it.copy(musicStyle = MusicStyle.COVER) } }, Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(40.dp).background(Color.White.copy(alpha = .35f), RoundedCornerShape(8.dp)))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Bars(46, 30, height = 4)
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(Color.White.copy(alpha = .5f), CircleShape))
                                Box(Modifier.size(12.dp).border(1.dp, Color.White.copy(alpha = .7f), CircleShape))
                                Box(Modifier.size(6.dp).background(Color.White.copy(alpha = .5f), CircleShape))
                            }
                        }
                    }
                }
            }
        }

    }
    SettingsCard {
        Section("Gestos no fundo")
        GesturesSection(settings, apps, onChange)

    }
}

@Composable
private fun AppsPage(settings: LauncherSettings, apps: Map<AppKey, AppEntry>, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    SettingsCard {
        Section("Favoritos")
        ToggleRow("Favoritos por horário", "Listas diferentes de dia, no trabalho (seg. a sex., 8h às 18h) e à noite (19h às 7h).", settings.timeProfiles) { v -> onChange { it.copy(timeProfiles = v) } }
        var editing by rememberSaveable { mutableStateOf(com.brendon978.menulauncher.settings.currentProfile()) }
        if (settings.timeProfiles) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                com.brendon978.menulauncher.settings.TimeProfile.entries.forEach { p -> Chip(p.label, editing == p) { editing = p } }
            }
        }
        val shownFavorites = settings.activeFavorites(editing)
        if (shownFavorites.isEmpty()) {
            Text("Nenhum. Segure um app na tela inicial para favoritar.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        shownFavorites.forEachIndexed { i, key ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(apps[key]?.label ?: key.packageName, color = Color.White, modifier = Modifier.weight(1f))
                SmallButton("↑", enabled = i > 0) { onChange { s -> s.editFavorites(editing) { FavoritesOps.move(it, i, i - 1) } } }
                SmallButton("↓", enabled = i < shownFavorites.lastIndex) { onChange { s -> s.editFavorites(editing) { FavoritesOps.move(it, i, i + 1) } } }
                SmallButton("✕", enabled = true) { onChange { s -> s.editFavorites(editing) { FavoritesOps.toggle(it, key) } } }
            }
        }
        if (settings.timeProfiles) {
            Text("Para adicionar a outro horário, segure um app enquanto estiver nele; aqui dá para ordenar e tirar.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        AddFavorite(settings, apps, onChange)

    }
    SettingsCard { HiddenAppsSection(settings, apps, onChange) }
}

@Composable
private fun GlitchPage(settings: LauncherSettings, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    SettingsCard {
        Section("Na tela inicial")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Glitch na tela inicial", color = Color.White, modifier = Modifier.weight(1f))
            Switch(settings.showPet, onCheckedChange = { v -> onChange { it.copy(showPet = v) } })
        }
        if (settings.showPet) {
            ToggleRow("Glitch calmo", "Plantinha e torrinha só aparecem enquanto ele brinca com elas.", settings.glitchCalm) { v -> onChange { it.copy(glitchCalm = v) } }
            Text("Acessório", color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.brendon978.menulauncher.pet.Accessory.entries.filter { it.choosable }.forEach { a ->
                    Chip(a.label, settings.glitchAccessory == a.name) { onChange { it.copy(glitchAccessory = a.name) } }
                }
            }
            Text("Bichinho", color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.brendon978.menulauncher.pet.Buddy.entries.forEach { p ->
                    Chip(p.label, settings.glitchBuddy == p.name) { onChange { it.copy(glitchBuddy = p.name) } }
                }
            }
        }
    }
    if (settings.showPet) {
        SettingsCard { GlitchPanel() }
        SettingsCard {
            Section("Comportamento")
            ToggleRow("Comentários", "Ele comenta o app de onde você voltou, de vez em quando.", settings.glitchComments) { v -> onChange { it.copy(glitchComments = v) } }
            ToggleRow("Travessuras", "Ele entorta o relógio e empurra um favorito; cutucar faz ele arrumar.", settings.glitchMischief) { v -> onChange { it.copy(glitchMischief = v) } }
            var birthdayText by rememberSaveable { mutableStateOf(settings.birthday) }
            OutlinedTextField(
                value = birthdayText,
                onValueChange = { v ->
                    birthdayText = v
                    if (v.isBlank() || com.brendon978.menulauncher.pet.parseBirthday(v) != null) onChange { it.copy(birthday = v.trim()) }
                },
                singleLine = true,
                label = { Text("Seu aniversário (dia/mês)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            var cityText by rememberSaveable { mutableStateOf(settings.weatherCity) }
            OutlinedTextField(
                value = cityText,
                onValueChange = { v -> cityText = v; onChange { it.copy(weatherCity = v) } },
                singleLine = true,
                label = { Text("Sua cidade, para o clima (opcional)") },
                supportingText = { Text("Só o nome da cidade vai para o Open-Meteo. Vazio, ele não consulta nada.") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        SettingsCard { GlitchReset() }
    }
}

@Composable
private fun SystemPage(
    settings: LauncherSettings,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onSetDefaultLauncher: () -> Unit,
) {
    SettingsCard {
        Section("Backup")
        BackupSection(settings, onChange)
    }
    SettingsCard {
        Section("Sistema")
        // Sem UpdateRow neste fork: o atualizador baixa o APK do Brendon, sem as alterações daqui.
        AccessibilityRow()
        Text(
            "Definir como tela inicial padrão",
            color = Color.White,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSetDefaultLauncher).padding(vertical = 12.dp),
        )
    }
}

@Composable
internal fun Section(title: String) {
    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        COLOR_CHOICES.forEach { c ->
            Box(
                Modifier
                    .size(36.dp)
                    .background(Color(c), CircleShape)
                    .border(if (c == selected) LocalBorderWidth.current * 3 else LocalBorderWidth.current, Color.White.copy(alpha = if (c == selected) 1f else 0.3f), CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
internal fun Chip(text: String, selected: Boolean, fontFamily: FontFamily? = null, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.Black else Color.White,
        fontFamily = fontFamily,
        // A sombra da tela inicial borra o texto escuro no chip selecionado.
        style = LocalTextStyle.current.copy(shadow = null),
        modifier = Modifier
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .border(LocalBorderWidth.current, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun SmallButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(text, color = Color.White.copy(alpha = if (enabled) 0.9f else 0.25f))
    }
    Spacer(Modifier.width(2.dp))
}

/** O cérebro do Glitch ao vivo: o que ele está fazendo, as vontades e o vínculo com você. */
@Composable
private fun GlitchPanel() {
    val snap by com.brendon978.menulauncher.pet.GlitchLive.state.collectAsState()
    val s = snap
    if (s == null) {
        Text("Abra a tela inicial no modo uma mão para acordar o Glitch.", color = Color.White.copy(alpha = .6f), fontSize = 13.sp)
        return
    }
    Text(s.doing, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
    Bar("Energia", s.drives.energy)
    Bar("Tédio", s.drives.boredom)
    Bar("Carência", s.drives.longing)
    Bar("Curiosidade", s.drives.curiosity)
    Bar("Irritação", s.irritation.coerceIn(0f, 1f))
    Bar("Vínculo com você", s.bond)
    if (s.diary.isNotBlank()) {
        Text("Diário de hoje", color = Color.White.copy(alpha = .55f), fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
        Text("“${s.diary}”", color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun Bar(label: String, value: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.width(130.dp))
        Box(Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(8.dp).background(Color.White.copy(alpha = .85f), RoundedCornerShape(4.dp)))
        }
        Text("${(value * 100).toInt()}%", color = Color.White.copy(alpha = .6f), fontSize = 12.sp, modifier = Modifier.width(44.dp).padding(start = 8.dp))
    }
}

/** Procura versão nova no GitHub; instalar sempre passa pela confirmação do Android. */
@Composable
private fun UpdateRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by androidx.compose.runtime.remember { mutableStateOf("Versão ${com.brendon978.menulauncher.BuildConfig.VERSION_NAME}") }
    var busy by androidx.compose.runtime.remember { mutableStateOf(false) }
    Text(
        "Procurar atualização",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
            busy = true
            scope.launch {
                val r = com.brendon978.menulauncher.update.Updater.latest()
                when {
                    r == null -> status = "Não consegui ver agora"
                    !com.brendon978.menulauncher.update.isNewer(r, com.brendon978.menulauncher.BuildConfig.VERSION_CODE) -> status = "Já está na versão mais nova"
                    !context.packageManager.canRequestPackageInstalls() -> {
                        status = "Libere instalar apps do menu-launcher e toque de novo"
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:" + context.packageName)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    else -> {
                        status = "Baixando…"
                        runCatching { com.brendon978.menulauncher.update.Updater.install(context, com.brendon978.menulauncher.update.Updater.download(context, r)) }
                            .onSuccess { status = r.notes.ifBlank { "Pronto para instalar" } }
                            .onFailure { status = "O download falhou" }
                    }
                }
                busy = false
            }
        }.padding(top = 8.dp, bottom = 2.dp),
    )
    Text(status, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
}

/** Volta o Glitch ao zero: vontades, vínculo, diário, aprendizado, rancor e protestos. Acessório e ajustes ficam. */
@Composable
private fun GlitchReset() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var asking by androidx.compose.runtime.remember { mutableStateOf(false) }
    Text(
        "Zerar o Glitch",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable { asking = true }.padding(vertical = 12.dp),
    )
    if (asking) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text("Zerar o Glitch?") },
            text = { Text("Ele esquece tudo: vínculo, vontades, diário, o que aprendeu e o que você aprontou. Não dá para desfazer.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    asking = false
                    scope.launch {
                        val live = com.brendon978.menulauncher.pet.GlitchLive
                        live.importing = true // o Glitch da tela não pode salvar por cima
                        com.brendon978.menulauncher.pet.GlitchStore(context.applicationContext)
                            .save(com.brendon978.menulauncher.pet.GlitchMemory(lastSeen = System.currentTimeMillis()))
                        (context as? android.app.Activity)?.recreate()
                    }
                }) { Text("Zerar") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { asking = false }) { Text("Cancelar") } },
        )
    }
}

/** Exportar e importar ajustes (e a memória do Glitch) num arquivo de texto. */
@Composable
private fun BackupSection(settings: LauncherSettings, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val store = androidx.compose.runtime.remember { com.brendon978.menulauncher.pet.GlitchStore(context.applicationContext) }
    fun toast(m: String) = android.widget.Toast.makeText(context, m, android.widget.Toast.LENGTH_SHORT).show()
    val export = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = Backup.encode(settings, com.brendon978.menulauncher.pet.GlitchBackup.toExtras(store.load()))
            runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
                .onSuccess { toast("Backup salvo") }.onFailure { toast("Não consegui salvar") }
        }
    }
    val import = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
            val restored = text?.let { Backup.decode(it) }
            if (restored == null) { toast("Esse arquivo não é um backup do launcher"); return@launch }
            com.brendon978.menulauncher.pet.GlitchBackup.fromExtras(Backup.extras(text, "glitch."))?.let { mem ->
                com.brendon978.menulauncher.pet.GlitchLive.importing = true
                store.save(mem)
            }
            // não traz a primeira abertura de volta
            onChange { restored.copy(firstRunDone = true) }
            toast("Backup restaurado")
            (context as? android.app.Activity)?.recreate()
        }
    }
    Text(
        "Exportar ajustes e o Glitch",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable { export.launch("menu-launcher-backup.txt") }.padding(vertical = 12.dp),
    )
    Text(
        "Importar de um arquivo",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable { import.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) }.padding(vertical = 12.dp),
    )
    Text(
        "Favoritos, cores, nomes, apps escondidos, gestos, nota, IA, acessório e a memória do Glitch (vínculo e vontades).",
        color = Color.White.copy(alpha = .5f),
        fontSize = 12.sp,
    )
}

@Composable
internal fun ToggleRow(title: String, hint: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(hint, color = Color.White.copy(alpha = .5f), fontSize = 12.sp)
        }
        Switch(value, onCheckedChange = onToggle)
    }
}

/** Ligar sem o acesso leva à tela do sistema; enquanto ele não for liberado, a tela inicial não mostra nada. */
@Composable
private fun ScreenTimeToggle(value: Boolean, hasAccess: Boolean, onToggle: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hint = if (value && !hasAccess) "Falta liberar o menu-launcher em Acesso a uso de apps." else "Discreto, embaixo da data (pede o acesso a uso de apps)."
    ToggleRow("Tempo de tela", hint, value) { v ->
        onToggle(v)
        if (v && !hasAccess) runCatching {
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/** Ligar o próximo compromisso pede acesso à agenda na hora. */
@Composable
private fun ContactsToggle(value: Boolean, onToggle: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        onToggle(granted)
    }
    ToggleRow("Contatos na busca", "Encontra pessoas ao buscar (pede acesso aos contatos).", value) { v ->
        if (!v) onToggle(false)
        else if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) onToggle(true)
        else ask.launch(android.Manifest.permission.READ_CONTACTS)
    }
}

@Composable
private fun NextEventToggle(value: Boolean, onToggle: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        onToggle(granted)
        if (!granted) android.widget.Toast.makeText(context, "Sem acesso à agenda não dá pra mostrar o compromisso", android.widget.Toast.LENGTH_SHORT).show()
    }
    ToggleRow("Próximo compromisso", "Da sua agenda, embaixo da data (pede acesso à agenda).", value) { v ->
        if (!v) onToggle(false)
        else if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) onToggle(true)
        else ask.launch(android.Manifest.permission.READ_CALENDAR)
    }
}

/** Campo de busca da tela principal dos Ajustes; com texto, a lista de opções achadas no lugar dos temas. */
@Composable
private fun SettingsSearch(query: String, onQuery: (String) -> Unit, onPick: (SettingsHit) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text("Buscar nos ajustes", color = Color.White.copy(alpha = .45f)) },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
    if (query.isBlank()) return
    val hits = remember(query) { SettingsIndex.search(query) }
    if (hits.isEmpty()) {
        Text("Nada com \"$query\".", color = Color.White.copy(alpha = .55f), fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
    }
    hits.forEach { hit ->
        val title = SettingsPage.valueOf(hit.page).title
        Column(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cardFill(LocalGlass.current), RoundedCornerShape(14.dp))
                .clickable { onPick(hit) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(hit.label, color = Color.White, fontSize = 15.sp)
            Text(title, color = Color.White.copy(alpha = .5f), fontSize = 12.sp)
        }
    }
}

/** Faixa no pé da tela com uma ação de desfazer. */
@Composable
private fun UndoBar(text: String, action: String, onAction: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(Color(0xFF2A2A30), RoundedCornerShape(14.dp))
            .padding(start = 16.dp, end = 4.dp),
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(action, color = Color(0xFF9D8CFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction).padding(14.dp))
    }
}
