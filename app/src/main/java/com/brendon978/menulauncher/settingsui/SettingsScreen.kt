package com.brendon978.menulauncher.settingsui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings

/** Toque longo numa área vazia abre aqui. Tudo se ajusta no celular. */
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    apps: Map<AppKey, AppEntry>,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    aiSection: @Composable () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ajustes", color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text("Fechar", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }

        Section("Modo")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Uma mão", settings.mode == HomeMode.ONE_HAND) { onChange { it.copy(mode = HomeMode.ONE_HAND) } }
            Chip("Grade", settings.mode == HomeMode.GRID) { onChange { it.copy(mode = HomeMode.GRID) } }
        }

        SizeSection(settings, onChange)
        ClockSection(settings, onChange)

        Section("Cor do traço")
        ColorSourceChoice(settings, onChange)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            COLOR_CHOICES.forEach { c ->
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Color(c), CircleShape)
                        .border(if (c == settings.strokeColor) 3.dp else 1.dp, Color.White.copy(alpha = if (c == settings.strokeColor) 1f else 0.3f), CircleShape)
                        .clickable { onChange { it.copy(strokeColor = c) } },
                )
            }
        }
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

        IconLookSection(settings, onChange)
        FontSection(settings, onChange)
        BarSideSection(settings, onChange)
        GridSection(settings, onChange)
        GestureSection(settings, onChange)

        Section("Música")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar o que está tocando", color = Color.White, modifier = Modifier.weight(1f))
            Switch(settings.showMusic, onCheckedChange = { v -> onChange { it.copy(showMusic = v) } })
        }

        Section("Favoritos")
        if (settings.favorites.isEmpty()) {
            Text("Nenhum. Segure um app na tela inicial para favoritar.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        settings.favorites.forEachIndexed { i, key ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(apps[key]?.label ?: key.packageName, color = Color.White, modifier = Modifier.weight(1f))
                SmallButton("↑", enabled = i > 0) { onChange { it.copy(favorites = FavoritesOps.move(it.favorites, i, i - 1)) } }
                SmallButton("↓", enabled = i < settings.favorites.lastIndex) { onChange { it.copy(favorites = FavoritesOps.move(it.favorites, i, i + 1)) } }
                SmallButton("✕", enabled = true) { onChange { it.copy(favorites = FavoritesOps.toggle(it.favorites, key)) } }
            }
        }

        HiddenAppsSection(settings, apps, onChange)

        Section("Robozinho")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Robozinho na tela inicial", color = Color.White, modifier = Modifier.weight(1f))
            Switch(settings.showPet, onCheckedChange = { v -> onChange { it.copy(showPet = v) } })
        }

        Section("IA")
        aiSection()

        Section("Papel de parede")
        WallpaperSection()

        BackupSection(settings, onChange)

        Section("Sistema")
        Text(
            "Definir como tela inicial padrão",
            color = Color.White,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSetDefaultLauncher).padding(vertical = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun Section(title: String) {
    Text(title, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
}

@Composable
internal fun Chip(text: String, selected: Boolean, fontFamily: FontFamily? = null, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.Black else Color.White,
        fontFamily = fontFamily,
        modifier = Modifier
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
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
