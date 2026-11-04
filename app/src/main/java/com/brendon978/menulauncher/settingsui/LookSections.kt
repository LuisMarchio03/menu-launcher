package com.brendon978.menulauncher.settingsui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.clock.ZONE_CHOICES
import com.brendon978.menulauncher.gestures.LauncherAccessibility
import com.brendon978.menulauncher.home.family
import com.brendon978.menulauncher.settings.BarSide
import com.brendon978.menulauncher.settings.ClockStyle
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

@Composable
fun ClockSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Relógio")
    OptionChips(
        listOf(
            "Fino" to ClockStyle.THIN,
            "Negrito empilhado" to ClockStyle.STACKED,
            "Analógico" to ClockStyle.ANALOG,
            "Por extenso" to ClockStyle.WORDS,
        ),
        settings.clockStyle,
    ) { v -> onChange { it.copy(clockStyle = v) } }
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
fun ColorSourceChoice(settings: LauncherSettings, onChange: OnChange) {
    OptionChips(listOf("Fixa" to ColorSource.FIXED, "Do papel de parede" to ColorSource.WALLPAPER), settings.colorSource) { v ->
        onChange { it.copy(colorSource = v) }
    }
    if (settings.colorSource == ColorSource.WALLPAPER) {
        Hint("A cor acompanha o papel de parede e muda quando ele muda. A cor fixa abaixo volta a valer em \"Fixa\".")
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun IconLookSection(settings: LauncherSettings, onChange: OnChange) {
    Section("Ícones e texto")
    ScaleSlider("Espessura do traço", settings.strokeScale, 0.5f..2f) { v -> onChange { it.copy(strokeScale = v) } }
    SwitchRow("Sombra (para papel de parede claro)", settings.textShadow) { v -> onChange { it.copy(textShadow = v) } }
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

private val GESTURE_ACTIONS = listOf(
    "Nada" to GestureAction.NONE,
    "Notificações" to GestureAction.NOTIFICATIONS,
    "Buscar app" to GestureAction.SEARCH,
    "Bloquear" to GestureAction.LOCK,
    "Ajustes" to GestureAction.SETTINGS,
)

@Composable
fun GestureSection(settings: LauncherSettings, onChange: OnChange) {
    val context = LocalContext.current
    Section("Gestos na tela inicial")
    SubLabel("Arrastar para baixo")
    OptionChips(GESTURE_ACTIONS, settings.swipeDown) { v -> onChange { it.copy(swipeDown = v) } }
    SubLabel("Arrastar para cima")
    OptionChips(GESTURE_ACTIONS, settings.swipeUp) { v -> onChange { it.copy(swipeUp = v) } }
    SubLabel("Toque duplo")
    OptionChips(GESTURE_ACTIONS, settings.doubleTap) { v -> onChange { it.copy(doubleTap = v) } }

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
        Text("Nenhum. Segure um app e toque em \"Esconder app\".", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
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
