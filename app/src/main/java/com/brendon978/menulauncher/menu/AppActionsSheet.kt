package com.brendon978.menulauncher.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Toque longo num app: opções, numa folha que sobe de baixo (zona do polegar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    label: String,
    isFavorite: Boolean,
    canUninstall: Boolean,
    onToggleFavorite: () -> Unit,
    onChangeIcon: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
    isHidden: Boolean = false,
    onRename: (() -> Unit)? = null,
    onToggleHidden: (() -> Unit)? = null,
    folderName: String? = null,
    onFolder: (() -> Unit)? = null,
    onLeaveFolder: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF18181B),
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Action(if (isFavorite) "Remover dos favoritos" else "Favoritar") { onToggleFavorite(); onDismiss() }
            Action("Trocar ícone") { onChangeIcon(); onDismiss() }
            onRename?.let { r -> Action("Renomear") { r(); onDismiss() } }
            onToggleHidden?.let { h -> Action(if (isHidden) "Mostrar na tela inicial" else "Esconder") { h(); onDismiss() } }
            if (folderName != null) onLeaveFolder?.let { l -> Action("Tirar da pasta \"$folderName\"") { l(); onDismiss() } }
            else onFolder?.let { f -> Action("Colocar em pasta") { f(); onDismiss() } }
            Action("Informações do app") { onAppInfo(); onDismiss() }
            if (canUninstall) Action("Desinstalar") { onUninstall(); onDismiss() }
        }
    }
}

@Composable
private fun Action(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

/** Caixinha para dar outro nome ao app (em branco volta ao original). */
@Composable
fun RenameDialog(original: String, current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    // começa com o nome atual (ou o original), cursor no fim e teclado aberto
    val start = current.ifBlank { original }
    var value by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(start, androidx.compose.ui.text.TextRange(start.length)))
    }
    val focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B),
        title = { Text("Renomear", color = Color.White) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text(original) },
                supportingText = { Text("Em branco volta para \"$original\"", color = Color.White.copy(alpha = .5f)) },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                // igual ao original conta como "sem nome próprio"
                onSave(value.text.takeIf { it.trim() != original }.orEmpty()); onDismiss()
            }) { Text("Salvar", color = Color.White) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(alpha = .7f)) } },
    )
}

/** Escolher uma pasta existente ou criar uma nova (as pastas aparecem no modo grade). */
@Composable
fun FolderDialog(existing: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B),
        title = { Text("Colocar em pasta", color = Color.White) },
        text = {
            Column {
                existing.forEach { f ->
                    Text(f, color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable { onPick(f); onDismiss() }.padding(vertical = 10.dp))
                }
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Nova pasta") },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = name.isNotBlank(), onClick = { onPick(name.trim()); onDismiss() }) { Text("Criar", color = Color.White) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(alpha = .7f)) } },
    )
}
