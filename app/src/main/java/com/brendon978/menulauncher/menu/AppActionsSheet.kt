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
import androidx.compose.ui.Modifier
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
    onHide: () -> Unit,
    onDismiss: () -> Unit,
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
            Action("Esconder app") { onHide(); onDismiss() }
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
