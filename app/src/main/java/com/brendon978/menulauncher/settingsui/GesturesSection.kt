package com.brendon978.menulauncher.settingsui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.settings.GestureAction
import com.brendon978.menulauncher.settings.GestureChoice
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.doubleTapChoice
import com.brendon978.menulauncher.settings.withDoubleTap

/** Deslizar para baixo, para cima e dois toques no fundo, cada um com a sua ação. */
@Composable
fun GesturesSection(
    settings: LauncherSettings,
    apps: Map<AppKey, AppEntry>,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
) {
    GestureRow("Deslizar para baixo", settings.gestureDown, apps) { c -> onChange { it.copy(gestureDown = c) } }
    GestureRow("Deslizar para cima", settings.gestureUp, apps) { c -> onChange { it.copy(gestureUp = c) } }
    GestureRow("Dois toques no fundo", settings.doubleTapChoice(), apps) { c -> onChange { it.withDoubleTap(c) } }
    val usesLock = listOf(settings.gestureDown, settings.gestureUp, settings.doubleTapChoice()).any { it.action == GestureAction.LOCK }
    if (usesLock) {
        Text(
            "Apagar a tela precisa ligar o menu-launcher em Configurações → Acessibilidade (ele só apaga a tela, não lê nada).",
            color = Color.White.copy(alpha = .5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureRow(title: String, choice: GestureChoice, apps: Map<AppKey, AppEntry>, onPick: (GestureChoice) -> Unit) {
    var pickingApp by remember { mutableStateOf(false) }
    Text(title, color = Color.White, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GestureAction.entries.forEach { a ->
            val label = if (a == GestureAction.APP && choice.action == a) choice.app?.let { apps[it]?.label ?: it.packageName } ?: a.label else a.label
            Chip(label, choice.action == a) {
                if (a == GestureAction.APP) pickingApp = true else onPick(GestureChoice(a))
            }
        }
    }
    if (pickingApp) {
        ModalBottomSheet(
            onDismissRequest = { pickingApp = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF18181B),
        ) {
            val sorted = remember(apps) { apps.values.sortedBy { it.label.lowercase() } }
            LazyColumn(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                items(sorted, key = { it.key.encode() }) { entry ->
                    Text(
                        entry.label,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickingApp = false
                                onPick(GestureChoice(GestureAction.APP, entry.key))
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}
