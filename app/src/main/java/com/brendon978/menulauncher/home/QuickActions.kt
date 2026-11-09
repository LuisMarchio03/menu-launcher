package com.brendon978.menulauncher.home

import androidx.compose.ui.graphics.toArgb
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.QuickStyle
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Lanterna, Wi-Fi, internet e não perturbe, em contorno, embaixo da data. */
@Composable
fun QuickActions(color: Color, settings: LauncherSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val torch = rememberTorch()
    val nm = remember { context.getSystemService(NotificationManager::class.java) }
    val dnd = com.brendon978.menulauncher.pet.rememberDoNotDisturb()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val plain = settings.quickStyle == QuickStyle.PLAIN
    val dim = settings.detailAlpha
    // sem círculo cada alvo continua com 40dp para o dedo, mas o desenho fica leve
    Row(modifier.padding(top = 10.dp * settings.spacing), horizontalArrangement = Arrangement.spacedBy(if (plain) 6.dp else 10.dp)) {
        @Composable
        fun QuickButton(color: Color, active: Boolean, onClick: () -> Unit, icon: DrawScope.(Color, Float) -> Unit) =
            if (plain) PlainButton(color, dim, active, settings.iconHalo, onClick, icon) else CircleButton(color, active, onClick, icon)
        QuickButton(color, active = torch.on, onClick = torch.toggle) { c, lw -> flashlight(c, lw) }
        QuickButton(color, active = false, onClick = { context.startPanel(Settings.Panel.ACTION_WIFI) }) { c, lw -> wifi(c, lw) }
        QuickButton(color, active = false, onClick = { context.startPanel(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) }) { c, lw -> globe(c, lw) }
        QuickButton(color, active = dnd, onClick = {
            if (nm.isNotificationPolicyAccessGranted) {
                val next = if (dnd) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY
                nm.setInterruptionFilter(next)
                // desde o Android 15 o app só controla a própria regra: o que veio da cortina ou de um agendamento segue ligado
                if (dnd) scope.launch {
                    kotlinx.coroutines.delay(600)
                    if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                        Toast.makeText(context, "Esse não perturbe veio de fora: desligue pela cortina", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Libere o acesso ao não perturbe para o menu-launcher", Toast.LENGTH_LONG).show()
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }) { c, lw -> moon(c, lw) }
    }
}

private fun Context.startPanel(action: String) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Só o desenho, apagado; ligado, ganha a cor cheia e um ponto embaixo. */
@Composable
private fun PlainButton(color: Color, dim: Float, active: Boolean, halo: Boolean, onClick: () -> Unit, icon: DrawScope.(Color, Float) -> Unit) {
    val lw = LocalLineScale.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
    ) {
        Canvas(Modifier.size(20.dp)) {
            // halo: o mesmo desenho mais grosso no tom oposto, por baixo, para ler sobre papel com detalhe
            if (halo) icon(Color(com.brendon978.menulauncher.wallpaper.haloFor(color.toArgb())).copy(alpha = .45f), lw * 2.6f)
            icon(color.copy(alpha = if (active) 1f else dim), lw)
            if (active) drawCircle(color, size.width * .07f, Offset(size.width / 2, size.height + size.width * .3f))
        }
    }
}

@Composable
private fun CircleButton(color: Color, active: Boolean, onClick: () -> Unit, icon: DrawScope.(Color, Float) -> Unit) {
    val lw = LocalLineScale.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (active) color else Color.Transparent, CircleShape)
            .border(LocalBorderWidth.current, color.copy(alpha = .4f), CircleShape)
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.size(18.dp)) { icon(if (active) Color(0xFF0C0C0E) else color, lw) }
    }
}

private fun DrawScope.st(lw: Float) = Stroke(size.width * .09f * lw, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.flashlight(c: Color, lw: Float) {
    val w = size.width
    drawPath(Path().apply { moveTo(w * .3f, w * .08f); lineTo(w * .7f, w * .08f); lineTo(w * .62f, w * .38f); lineTo(w * .38f, w * .38f); close() }, c, style = st(lw))
    drawRoundRect(c, Offset(w * .38f, w * .38f), Size(w * .24f, w * .56f), androidx.compose.ui.geometry.CornerRadius(w * .05f), style = st(lw))
    drawLine(c, Offset(w * .5f, w * .52f), Offset(w * .5f, w * .64f), size.width * .09f * lw, StrokeCap.Round)
}

private fun DrawScope.wifi(c: Color, lw: Float) {
    val w = size.width
    for ((i, r) in listOf(.46f, .32f, .18f).withIndex()) {
        drawArc(c, 225f, 90f, false, Offset(w * .5f - w * r, w * .82f - w * r), Size(w * r * 2, w * r * 2), style = st(lw))
        if (i == 2) drawCircle(c, w * .06f, Offset(w * .5f, w * .82f))
    }
}

private fun DrawScope.globe(c: Color, lw: Float) {
    val w = size.width
    drawCircle(c, w * .44f, Offset(w / 2, w / 2), style = st(lw))
    drawOval(c, Offset(w * .3f, w * .06f), Size(w * .4f, w * .88f), style = st(lw))
    drawLine(c, Offset(w * .06f, w / 2), Offset(w * .94f, w / 2), size.width * .09f * lw, StrokeCap.Round)
}

private fun DrawScope.moon(c: Color, lw: Float) {
    val w = size.width
    drawPath(Path().apply {
        moveTo(w * .62f, w * .08f)
        cubicTo(w * .2f, w * .1f, w * .02f, w * .62f, w * .4f, w * .88f)
        cubicTo(w * .66f, w * 1.04f, w * .94f, w * .82f, w * .94f, w * .64f)
        cubicTo(w * .66f, w * .76f, w * .4f, w * .46f, w * .62f, w * .08f)
        close()
    }, c, style = st(lw))
}

internal class Torch(val on: Boolean, val toggle: () -> Unit)

/** Lanterna: liga e desliga pela câmera com flash; acompanha se outro app mexer. */
@Composable
internal fun rememberTorch(): Torch {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(CameraManager::class.java) }
    val id = remember {
        runCatching { cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    DisposableEffect(cm, id) {
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if (cameraId == id) on = enabled }
        }
        cm.registerTorchCallback(cb, Handler(Looper.getMainLooper()))
        onDispose { cm.unregisterTorchCallback(cb) }
    }
    return Torch(on) { id?.let { runCatching { cm.setTorchMode(it, !on) } } }
}
