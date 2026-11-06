package com.brendon978.menulauncher.settingsui

import android.app.WallpaperColors
import android.app.WallpaperManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.wallpaper.autoStroke
import com.brendon978.menulauncher.wallpaper.legibleStroke
import com.brendon978.menulauncher.wallpaper.supportsDarkText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * As cores que o sistema informa do papel de parede da tela inicial e o traço automático que sai delas.
 * Tocar numa cor fixa o traço nela (já ajustada para contraste); "Ler de novo" relê do sistema.
 */
@Composable
fun WallpaperPalette(onPick: (Int) -> Unit) {
    val context = LocalContext.current
    var reads by remember { mutableIntStateOf(0) }
    val colors by produceState<Result<WallpaperColors?>?>(null, reads) {
        value = withContext(Dispatchers.IO) {
            runCatching { WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }
        }
    }
    val dim = Color.White.copy(alpha = .55f)
    Column(Modifier.padding(top = 14.dp)) {
        Text("Cores do papel de parede atual", color = Color.White.copy(alpha = .75f), fontSize = 13.sp)
        val read = colors ?: return@Column
        val wc = read.getOrNull()
        if (wc == null) {
            Text(
                "O sistema não informou cores deste papel (comum em papel animado de outro app). O traço automático usa a paleta do sistema.",
                color = dim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            val dark = supportsDarkText(wc)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                listOfNotNull(wc.primaryColor, wc.secondaryColor, wc.tertiaryColor).forEach { c ->
                    val raw = c.toArgb()
                    Swatch(raw) { onPick(legibleStroke(raw, dark)) }
                }
                Spacer(Modifier.width(6.dp))
                Text("→ traço", color = dim, fontSize = 12.sp)
                Swatch(autoStroke(wc, context.getColor(android.R.color.system_accent1_500)))
            }
            Text(
                "Toque numa cor para fixar o traço nela. Papel ${if (dark) "claro" else "escuro"}, segundo o sistema.",
                color = dim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text("Ler de novo", color = Color.White.copy(alpha = .8f), modifier = Modifier.clickable { reads++ }.padding(vertical = 10.dp))
    }
}

@Composable
private fun Swatch(argb: Int, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(36.dp)
            .background(Color(argb), CircleShape)
            .border(1.dp, Color.White.copy(alpha = .3f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}
