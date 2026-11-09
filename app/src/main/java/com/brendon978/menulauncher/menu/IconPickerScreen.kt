package com.brendon978.menulauncher.menu

import android.content.res.AssetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.icons.IconSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun lawniconNames(assets: AssetManager): List<String> =
    assets.list("lawnicons/svgs").orEmpty().filter { it.endsWith(".svg") }.map { it.removeSuffix(".svg") }.sorted()

private const val AUTO = ""

/** Grade do Lawnicons com busca; "Automático" (null) remove a troca. */
@Composable
fun IconPickerScreen(
    appLabel: String,
    renderer: IconRenderer,
    color: Int,
    assets: AssetManager,
    onPick: (String?) -> Unit,
) {
    // null = ainda lendo a lista (são mais de 10 mil desenhos: leva uns segundos na primeira vez)
    val loaded by produceState<List<String>?>(null) { value = withContext(Dispatchers.IO) { lawniconNames(assets) } }
    val all = loaded.orEmpty()
    var query by rememberSaveable { mutableStateOf(appLabel.lowercase().filter { it.isLetterOrDigit() }) }
    val shown = remember(all, query) {
        val q = query.trim().lowercase()
        listOf(AUTO) + (if (q.isEmpty()) all else all.filter { q in it })
    }
    Column(Modifier.fillMaxSize().background(com.brendon978.menulauncher.home.overlayBackground(com.brendon978.menulauncher.home.LocalGlass.current)).statusBarsPadding().padding(16.dp)) {
        Text("Ícone de $appLabel", color = Color.White, fontSize = 18.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Buscar ícone") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        if (loaded == null) Text("Carregando ícones…", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        else if (shown.size == 1 && query.isNotBlank()) Text("Nenhum ícone com \"$query\".", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(72.dp)) {
            items(shown, key = { it }) { name ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onPick(name.takeIf { it != AUTO }) }
                        .padding(8.dp),
                ) {
                    if (name == AUTO) {
                        Box(Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)))
                    } else {
                        LawnIcon(name, renderer, color)
                    }
                    Text(
                        if (name == AUTO) "Automático" else name,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LawnIcon(name: String, renderer: IconRenderer, color: Int) {
    val px = with(LocalDensity.current) { 32.dp.roundToPx() }
    val bmp by produceState<ImageBitmap?>(null, name, color) {
        value = withContext(Dispatchers.Default) { renderer.render(IconSource.Lawnicon(name), px, color) }
    }
    val b = bmp
    if (b == null) Box(Modifier.size(32.dp)) else Image(b, null, Modifier.size(32.dp))
}
