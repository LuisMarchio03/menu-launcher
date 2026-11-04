package com.brendon978.menulauncher.ai

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.home.LocalBorderWidth

/**
 * IAs compatíveis (declaram o contrato) primeiro; "Outro app" conecta qualquer app só com o toque de abrir.
 * [onPick] recebe pacote e Activity que abre no toque.
 */
@Composable
fun AiChooser(
    candidates: List<AiCandidate>,
    allApps: List<AppEntry>,
    connectedPackage: String?,
    onPick: (String, String) -> Unit,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    Column {
        if (candidates.isEmpty()) {
            Text(
                "Nenhuma IA compatível instalada.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        candidates.forEach { c ->
            Option(c.label, selected = c.packageName == connectedPackage) { onPick(c.packageName, c.activityName) }
        }
        Option(if (showAll) "Outro app ▴" else "Outro app ▾", selected = false) { showAll = !showAll }
        if (showAll) {
            val sorted = remember(allApps) { LetterIndex.sorted(allApps) }
            LazyColumn(Modifier.heightIn(max = 280.dp)) {
                items(sorted, key = { it.key.encode() }) { app ->
                    Text(
                        app.label,
                        color = if (app.key.packageName == connectedPackage) Color.White else Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(app.key.packageName, app.key.activityName) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Option(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .border(if (selected) LocalBorderWidth.current * 2 else LocalBorderWidth.current, Color.White.copy(alpha = if (selected) 1f else 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
