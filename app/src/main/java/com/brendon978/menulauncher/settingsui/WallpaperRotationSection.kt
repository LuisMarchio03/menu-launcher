package com.brendon978.menulauncher.settingsui

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.wallpaper.WallpaperInterval
import com.brendon978.menulauncher.wallpaper.folderLabel

/** Papel de parede que muda: uma pasta e o intervalo. A troca acontece ao voltar para a tela inicial. */
@Composable
fun WallpaperRotationSection(settings: LauncherSettings, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val folder = settings.wallpaperFolder
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val kept = runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess
        if (!kept) {
            Toast.makeText(context, "Não consegui guardar o acesso a essa pasta", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        if (folder.isNotEmpty() && folder != uri.toString()) {
            runCatching { resolver.releasePersistableUriPermission(Uri.parse(folder), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        onChange { it.copy(wallpaperFolder = uri.toString(), wallpaperLastImage = "", wallpaperLastChange = 0) }
    }
    // backup vindo de outro celular traz o endereço da pasta, mas não a permissão
    val status = remember(folder) {
        when {
            folder.isEmpty() -> "Nenhuma pasta escolhida"
            resolver.persistedUriPermissions.none { it.uri.toString() == folder } -> "Sem acesso a essa pasta: escolha de novo"
            else -> runCatching { folderLabel(DocumentsContract.getTreeDocumentId(Uri.parse(folder))) }.getOrDefault("Pasta escolhida")
        }
    }
    Text(
        "Papel de parede que muda",
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable { picker.launch(null) }.padding(top = 12.dp, bottom = 4.dp),
    )
    Text(
        "$status. Toque para escolher a pasta. Troca só a tela inicial, ao voltar para ela (tira vídeo ou GIF que esteja lá); o bloqueio fica como está.",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WallpaperInterval.entries.forEach { i ->
            Chip(i.label, settings.wallpaperInterval == i) { onChange { it.copy(wallpaperInterval = i) } }
        }
    }
}
