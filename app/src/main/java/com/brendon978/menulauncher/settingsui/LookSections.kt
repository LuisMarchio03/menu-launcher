package com.brendon978.menulauncher.settingsui

import com.brendon978.menulauncher.settings.IconShape
import com.brendon978.menulauncher.settings.BlockPosition
import com.brendon978.menulauncher.settings.ListAlign
import com.brendon978.menulauncher.settings.InitialStyle
import com.brendon978.menulauncher.settings.QuickReveal
import com.brendon978.menulauncher.settings.AppWeight
import com.brendon978.menulauncher.settings.EmptyNote
import com.brendon978.menulauncher.settings.LabelStyle
import com.brendon978.menulauncher.settings.QuickStyle
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import com.brendon978.menulauncher.ai.positionFraction
import com.brendon978.menulauncher.home.LocalBorderWidth
import com.brendon978.menulauncher.settings.HomeMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.clock.ZONE_CHOICES
import com.brendon978.menulauncher.gestures.LauncherAccessibility
import com.brendon978.menulauncher.home.family
import com.brendon978.menulauncher.settings.BarSide
import com.brendon978.menulauncher.settings.ClockAlign
import com.brendon978.menulauncher.settings.ClockStyle
import com.brendon978.menulauncher.settings.ClockTap
import com.brendon978.menulauncher.settings.HourFormat
import com.brendon978.menulauncher.settings.ColorSource
import com.brendon978.menulauncher.settings.DateStyle
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.FontChoice
import com.brendon978.menulauncher.settings.GestureAction
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.withBarSide
import kotlin.math.roundToInt

private typealias OnChange = ((LauncherSettings) -> LauncherSettings) -> Unit

private val WHITE = Color.White
private val DIM = Color.White.copy(alpha = 0.55f)

@Composable
private fun Choices(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** Uma linha de opções exclusivas: marca a [selected] e chama [onPick] com a escolhida. */
@Composable
private fun <T> OptionChips(options: List<Pair<String, T>>, selected: T, onPick: (T) -> Unit) {
    Choices { options.forEach { (label, v) -> Chip(label, selected == v) { onPick(v) } } }
}

@Composable
private fun SubLabel(text: String) {
    Text(text, color = DIM, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text(text, color = WHITE, modifier = Modifier.weight(1f))
        Switch(checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
}

/** Desliza à vontade e grava só ao soltar (gravar a cada quadro pesa no DataStore). */
@Composable
private fun ScaleSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onCommit: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = WHITE, modifier = Modifier.weight(1f))
        Text("${(local * 100).roundToInt()}%", color = DIM, fontSize = 13.sp)
    }
    Slider(
        value = local,
        onValueChange = { local = (it * 20).roundToInt() / 20f }, // passos de 5%
        valueRange = range,
        onValueChangeFinished = { onCommit(local) },
    )
}

@Composable
fun SizeSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Tamanho")
    ScaleSlider("Relógio", settings.clockScale, 0.6f..1.6f) { v -> onChange { it.copy(clockScale = v) } }
    ScaleSlider("Ícones", settings.iconScale, 0.75f..1.75f) { v -> onChange { it.copy(iconScale = v) } }
    ScaleSlider("Texto", settings.textScale, 0.8f..1.5f) { v -> onChange { it.copy(textScale = v) } }
    ScaleSlider("Altura das linhas", settings.rowScale, 0.8f..1.6f) { v -> onChange { it.copy(rowScale = v) } }
    ScaleSlider("Barra de letras", settings.barScale, 0.8f..1.5f) { v -> onChange { it.copy(barScale = v) } }
    Text(
        "Voltar ao tamanho original",
        color = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .clickable {
                val d = LauncherSettings()
                onChange {
                    it.copy(clockScale = d.clockScale, iconScale = d.iconScale, textScale = d.textScale, rowScale = d.rowScale, barScale = d.barScale)
                }
            }
            .padding(vertical = 10.dp),
    )
}

/** Onde e como os apps aparecem, em qualquer modo. */
@Composable
fun ShapeSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Forma e posição dos apps")
    SubLabel("Lista encostada")
    OptionChips(listOf("Esquerda" to ListAlign.START, "Centro" to ListAlign.CENTER, "Direita" to ListAlign.END), settings.listAlign) { v -> onChange { it.copy(listAlign = v) } }
    SubLabel("Bloco de apps (modo uma mão)")
    OptionChips(listOf("Embaixo" to BlockPosition.BOTTOM, "No meio" to BlockPosition.MIDDLE, "Em cima" to BlockPosition.TOP), settings.blockPosition) { v ->
        onChange { it.copy(blockPosition = v) }
    }
    SubLabel("Ícone")
    OptionChips(listOf("Só contorno" to IconShape.OUTLINE, "Círculo" to IconShape.CIRCLE, "Quadrado" to IconShape.SQUARE), settings.iconShape) { v -> onChange { it.copy(iconShape = v) } }
    SwitchRow("Nome dos apps", settings.showLabels) { v -> onChange { it.copy(showLabels = v) } }
    SwitchRow("Páginas: favoritos e pastas lado a lado", settings.favoritePages) { v -> onChange { it.copy(favoritePages = v) } }
    Hint("No modo uma mão, arraste a lista para o lado para trocar entre os favoritos e cada pasta.")
}

/** O que deixa a tela leve ou carregada: peso dos detalhes, títulos, atalhos, barra, nota e espaços. */
@Composable
fun FinishSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Acabamento")
    ScaleSlider("Força dos detalhes", settings.detailAlpha, 0.25f..1f) { v -> onChange { it.copy(detailAlpha = v) } }
    Hint("Atalhos, títulos, barra de letras e nota vazia. Mais baixo, os apps e o relógio aparecem mais.")
    ScaleSlider("Respiro entre os blocos", settings.spacing, 0.6f..1.8f) { v -> onChange { it.copy(spacing = v) } }
    SubLabel("Nome dos apps")
    OptionChips(listOf("Fino" to AppWeight.LIGHT, "Normal" to AppWeight.NORMAL), settings.appWeight) { v -> onChange { it.copy(appWeight = v) } }
    SubLabel("Títulos (Favoritos, Mais usados)")
    OptionChips(listOf("MAIÚSCULAS" to LabelStyle.CAPS, "Normal" to LabelStyle.NORMAL, "Sem título" to LabelStyle.HIDDEN), settings.labelStyle) { v ->
        onChange { it.copy(labelStyle = v) }
    }
    SubLabel("Atalhos rápidos")
    OptionChips(listOf("Só o desenho" to QuickStyle.PLAIN, "Em círculos" to QuickStyle.CIRCLES), settings.quickStyle) { v -> onChange { it.copy(quickStyle = v) } }
    SubLabel("Nota vazia")
    OptionChips(listOf("+ nota" to EmptyNote.PLUS, "Toque para anotar" to EmptyNote.TEXT), settings.emptyNote) { v -> onChange { it.copy(emptyNote = v) } }
    ScaleSlider("Véu escuro no topo e no pé", settings.edgeVeil, 0f..1f) { v -> onChange { it.copy(edgeVeil = v) } }
    SwitchRow("Data e tempo de tela numa linha", settings.compactInfo) { v -> onChange { it.copy(compactInfo = v) } }
    SubLabel("Atalhos rápidos aparecem")
    OptionChips(listOf("Ao tocar na data" to QuickReveal.ON_TAP, "Sempre" to QuickReveal.ALWAYS), settings.quickReveal) { v -> onChange { it.copy(quickReveal = v) } }
    SwitchRow("Movimento (entrada, toque que encolhe, vibração)", settings.motion) { v -> onChange { it.copy(motion = v) } }
    SwitchRow("Aro no símbolo da IA", settings.aiRing) { v -> onChange { it.copy(aiRing = v) } }
    ScaleSlider("Tamanho do símbolo da IA", settings.aiScale, 0.5f..1.5f) { v -> onChange { it.copy(aiScale = v) } }
    SwitchRow("Barra de letras em arco", settings.barCurved) { v -> onChange { it.copy(barCurved = v) } }
    SwitchRow("Sombra nos desenhos dos atalhos", settings.iconHalo) { v -> onChange { it.copy(iconHalo = v) } }
    SwitchRow("Vidro no fundo (Ajustes, busca)", settings.blurOverlays) { v -> onChange { it.copy(blurOverlays = v) } }
    Hint("O papel de parede aparece atrás de um véu escuro. Desfocado só onde o sistema deixa: o One UI não deixa.")
    Text(
        "Voltar ao acabamento original",
        color = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .clickable {
                onChange {
                    it.copy(
                        quickStyle = QuickStyle.CIRCLES, detailAlpha = 0.6f, labelStyle = LabelStyle.NORMAL,
                        emptyNote = EmptyNote.TEXT, barCurved = true, appWeight = AppWeight.NORMAL, spacing = 1f,
                        edgeVeil = 0f, compactInfo = false, quickReveal = QuickReveal.ALWAYS, motion = false, aiRing = true, aiScale = 1f,
                    )
                }
            }
            .padding(vertical = 10.dp),
    )
}

@Composable
fun ClockSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Relógio")
    ClockStylePicker(settings) { v -> onChange { it.copy(clockStyle = v) } }
    SubLabel("Formato")
    OptionChips(
        listOf("Do celular" to HourFormat.SYSTEM, "24h" to HourFormat.H24, "12h" to HourFormat.H12, "12h com AM/PM" to HourFormat.H12_AMPM),
        settings.hourFormat,
    ) { v -> onChange { it.copy(hourFormat = v) } }
    SwitchRow("Segundos", settings.clockSeconds) { v -> onChange { it.copy(clockSeconds = v) } }
    Hint("Nos relógios de dígitos. O relógio passa a atualizar a cada segundo.")
    SubLabel("Alinhamento (no modo uma mão, junto com o que vem embaixo)")
    OptionChips(
        listOf("Esquerda" to ClockAlign.START, "Centro" to ClockAlign.CENTER, "Direita" to ClockAlign.END),
        settings.clockAlign,
    ) { v -> onChange { it.copy(clockAlign = v) } }
    SubLabel("Tocar no relógio abre")
    OptionChips(
        listOf("Alarmes" to ClockTap.ALARMS, "Agenda" to ClockTap.CALENDAR, "Nada" to ClockTap.NONE),
        settings.clockTap,
    ) { v -> onChange { it.copy(clockTap = v) } }
    SubLabel("Data")
    OptionChips(
        listOf(
            "ter, 3 de nov." to DateStyle.SHORT,
            "terça-feira, 3 de novembro" to DateStyle.LONG,
            "03/11/2026" to DateStyle.NUMERIC,
            "Sem data" to DateStyle.HIDDEN,
        ),
        settings.dateStyle,
    ) { v -> onChange { it.copy(dateStyle = v) } }
    SubLabel("Segundo fuso")
    OptionChips(listOf<Pair<String, String?>>("Nenhum" to null) + ZONE_CHOICES.map { (id, label) -> label to id }, settings.secondZone) { v ->
        onChange { it.copy(secondZone = v) }
    }
}

@Composable
fun IconLookSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Ícones e texto")
    ScaleSlider("Espessura do traço dos ícones", settings.strokeScale, 0.5f..3f) { v -> onChange { it.copy(strokeScale = v) } }
    ScaleSlider("Espessura das bordas e do Glitch", settings.borderScale, 0.5f..3f) { v -> onChange { it.copy(borderScale = v) } }
    SwitchRow("Sombra (para papel de parede claro)", settings.textShadow) { v -> onChange { it.copy(textShadow = v) } }
    SubLabel("App sem desenho no pacote")
    OptionChips(listOf("Quadrado arredondado" to InitialStyle.SQUIRCLE, "Círculo" to InitialStyle.CIRCLE, "Só a letra" to InitialStyle.LETTER), settings.initialStyle) { v ->
        onChange { it.copy(initialStyle = v) }
    }
}

@Composable
fun FontSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Fonte")
    Choices {
        listOf(
            "Padrão" to FontChoice.SYSTEM,
            "Condensada" to FontChoice.CONDENSED,
            "Serifada" to FontChoice.SERIF,
            "Mono" to FontChoice.MONO,
            "Manrope" to FontChoice.MANROPE,
            "Outfit" to FontChoice.OUTFIT,
        ).forEach { (label, font) ->
            Chip(label, settings.font == font, fontFamily = font.family) { onChange { it.copy(font = font) } }
        }
    }
}

@Composable
fun BarSideSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Barra de letras (modo uma mão)")
    OptionChips(listOf("Direita" to BarSide.RIGHT, "Esquerda" to BarSide.LEFT), settings.barSide) { v ->
        onChange { it.withBarSide(v) }
    }
}

@Composable
fun GridSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Grade")
    OptionChips(listOf("Automático" to 0) + (3..6).map { "$it colunas" to it }, settings.gridColumns) { v ->
        onChange { it.copy(gridColumns = v) }
    }
    SwitchRow("Mostrar o nome dos apps", settings.gridLabels) { v -> onChange { it.copy(gridLabels = v) } }
}

@Composable
fun AccessibilityRow() {
    val context = LocalContext.current
    val on by LauncherAccessibility.isOn.collectAsState()
    Text(
        if (on) "Acessibilidade ligada: bloquear e notificações funcionam" else "Ligar a acessibilidade (para bloquear a tela e abrir as notificações)",
        color = if (on) DIM else WHITE,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .padding(vertical = 12.dp),
    )
    if (!on) Hint("Em Acessibilidade > Apps instalados > menu-launcher. Se a opção estiver bloqueada, abra Informações do app > ⋮ > Permitir configurações restritas.")
}

@Composable
fun HiddenAppsSection(settings: LauncherSettings, apps: Map<AppKey, AppEntry>, onChange: OnChange) {
    Section("Apps escondidos")
    if (settings.hiddenApps.isEmpty()) {
        Text("Nenhum. Segure um app e toque em \"Esconder\".", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
    }
    settings.hiddenApps.sortedBy { apps[it]?.label ?: it.packageName }.forEach { key ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(apps[key]?.label ?: key.packageName, color = WHITE, modifier = Modifier.weight(1f))
            Text(
                "Mostrar",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .clickable { onChange { FavoritesOps.setHidden(it, key, hidden = false) } }
                    .padding(12.dp),
            )
        }
    }
}

/** "Adicionar app": procura e favorita sem sair dos Ajustes; a folha fica aberta para escolher vários. */
@Composable
fun AddFavorite(settings: LauncherSettings, apps: Map<AppKey, AppEntry>, onChange: OnChange) {
    var open by remember { mutableStateOf(false) }
    Text(
        "Adicionar app",
        color = WHITE,
        modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 12.dp),
    )
    if (settings.mode == HomeMode.GRID) Hint("Os favoritos aparecem no modo uma mão; a grade mostra todos os apps.")
    if (open) FavoriteSheet(settings, apps, onChange) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteSheet(settings: LauncherSettings, apps: Map<AppKey, AppEntry>, onChange: OnChange, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val found = remember(apps, settings.favorites, settings.hiddenApps, query) { FavoritesOps.candidates(apps.values, settings, query) }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF18181B),
    ) {
        Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Buscar app") },
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(Modifier.padding(top = 8.dp).heightIn(max = 420.dp)) {
                items(found, key = { it.key.encode() }) { entry ->
                    Text(
                        entry.label,
                        color = WHITE,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChange { it.copy(favorites = FavoritesOps.add(it.favorites, entry.key)) } }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        }
    }
}

/** Mapa da tela: arraste ou toque para escolher onde o símbolo da IA fica (ele aparece no modo uma mão). */
@Composable
fun AiPositionPicker(settings: LauncherSettings, onChange: OnChange) {
    val config = LocalConfiguration.current
    val ratio = config.screenWidthDp / config.screenHeightDp.coerceAtLeast(1).toFloat()
    var spot by remember { mutableStateOf(Offset(settings.aiX, settings.aiY)) }
    LaunchedEffect(settings.aiX, settings.aiY) { spot = Offset(settings.aiX, settings.aiY) }
    val commit by rememberUpdatedState(onChange)
    val dot = 22.dp
    SubLabel("Posição do símbolo")
    BoxWithConstraints(
        Modifier
            .height(240.dp)
            .aspectRatio(ratio)
            .border(LocalBorderWidth.current, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                val dotPx = dot.toPx()
                fun place(p: Offset) {
                    spot = Offset(
                        positionFraction(p.x - dotPx / 2, size.width - dotPx),
                        positionFraction(p.y - dotPx / 2, size.height - dotPx),
                    )
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume() // o arrasto é do mapa, não da rolagem dos Ajustes
                    place(down.position)
                    do {
                        val change = awaitPointerEvent().changes.first()
                        change.consume()
                        place(change.position)
                    } while (change.pressed)
                    val chosen = spot
                    commit { it.copy(aiX = chosen.x, aiY = chosen.y) }
                }
            },
    ) {
        val free = with(LocalDensity.current) { IntOffset(constraints.maxWidth - dot.roundToPx(), constraints.maxHeight - dot.roundToPx()) }
        Box(
            Modifier
                // lido na fase de layout: arrastar não recompõe a tela
                .offset { IntOffset((free.x * spot.x).roundToInt(), (free.y * spot.y).roundToInt()) }
                .size(dot)
                .background(WHITE, CircleShape),
        )
    }
    Text(
        "Voltar ao lugar padrão",
        color = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .clickable {
                val d = LauncherSettings().withBarSide(settings.barSide)
                onChange { it.copy(aiX = d.aiX, aiY = d.aiY) }
            }
            .padding(vertical = 10.dp),
    )
    if (settings.mode == HomeMode.GRID) Hint("O símbolo só aparece no modo uma mão.")
}
