package com.brendon978.menulauncher.settingsui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.wallpaper.LockWallpaperService
import com.brendon978.menulauncher.wallpaper.StaticTarget
import com.brendon978.menulauncher.wallpaper.WallpaperApplier
import com.brendon978.menulauncher.wallpaper.WallpaperKind
import kotlinx.coroutines.launch

/** "Escolher imagem ou vídeo": estático pergunta onde aplicar; animado abre a prévia do sistema. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSection() {
    val context = LocalContext.current
    val applier = remember { WallpaperApplier(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var pendingStatic by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val kind = applier.kindOf(uri)) {
            WallpaperKind.Static -> pendingStatic = uri
            is WallpaperKind.Animated -> scope.launch {
                busy = true
                runCatching { applier.prepareAnimated(uri, kind.ext) }
                    .onSuccess { context.startActivity(applier.livePreviewIntent()) }
                    .onFailure { toast("Não consegui abrir esse arquivo") }
                busy = false
            }
            WallpaperKind.Unsupported -> toast("Formato não suportado")
        }
    }

    // o Glitch no bloqueio: escolhe o fundo e a prévia do sistema aplica
    val lockPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            runCatching { applier.prepareLockBackground(uri) }
                .onSuccess {
                    toast("Na tela seguinte, escolha Tela de bloqueio")
                    context.startActivity(applier.livePreviewIntent(LockWallpaperService::class.java))
                }
                .onFailure { toast("Não consegui abrir essa imagem") }
            busy = false
        }
    }

    Text(
        if (busy) "Aplicando…" else "Escolher imagem ou vídeo",
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !busy) { picker.launch(arrayOf("image/*", "video/*")) }
            .padding(vertical = 12.dp),
    )
    Text(
        "Imagem: tela inicial, de bloqueio ou as duas. Vídeo ou GIF: animado na tela inicial.",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
    )
    Text(
        "Glitch na tela de bloqueio",
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !busy) { lockPicker.launch(arrayOf("image/*")) }
            .padding(top = 12.dp, bottom = 4.dp),
    )
    Text(
        "Escolha a imagem de fundo. A digital e as notificações continuam normais.",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
    )

    pendingStatic?.let { uri ->
        ModalBottomSheet(
            onDismissRequest = { pendingStatic = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF18181B),
        ) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text("Aplicar em", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                listOf(
                    "Tela inicial" to StaticTarget.HOME,
                    "Tela de bloqueio" to StaticTarget.LOCK,
                    "As duas" to StaticTarget.BOTH,
                ).forEach { (label, target) ->
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pendingStatic = null
                                scope.launch {
                                    busy = true
                                    runCatching { applier.applyStatic(uri, target) }
                                        .onSuccess { toast("Papel de parede aplicado") }
                                        .onFailure { toast("Não consegui aplicar essa imagem") }
                                    busy = false
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}
