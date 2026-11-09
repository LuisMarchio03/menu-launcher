package com.brendon978.menulauncher.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.calc
import com.brendon978.menulauncher.apps.searchApps
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.LauncherSettings

/**
 * Buscador de apps: campo embaixo (zona do polegar, sobe com o teclado) e resultados logo acima,
 * o melhor mais perto do campo. "Ir" no teclado abre o primeiro resultado.
 */
@Composable
fun SearchScreen(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    strokeColor: Int,
    renderer: IconRenderer,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(apps, query) { searchApps(apps, query) }
    val math = remember(query) { calc(query) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val system = remember(query) { com.brendon978.menulauncher.apps.searchSystem(query) }
    val contacts by androidx.compose.runtime.produceState(emptyList<Pair<String, android.net.Uri>>(), query, settings.searchContacts) {
        value = if (settings.searchContacts) searchContacts(context, query) else emptyList()
    }
    val focus = remember { FocusRequester() }
    val look = settings.rowLook
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .background(com.brendon978.menulauncher.home.overlayBackground(com.brendon978.menulauncher.home.LocalGlass.current, solidAlpha = .9f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        LazyColumn(
            reverseLayout = true, // o primeiro resultado fica colado no campo
            modifier = Modifier.weight(1f, fill = false),
        ) {
            items(results, key = { it.key.encode() }) { entry ->
                AppRow(
                    entry = entry,
                    override = settings.iconOverrides[entry.key],
                    look = look,
                    renderer = renderer,
                    onClick = { onLaunch(entry) },
                    onLongClick = { onAppLongPress(entry) },
                )
            }
        }
        val lineColor = Color(strokeColor)
        contacts.forEach { (name, uri) ->
            ResultLine("Contato · $name", lineColor) {
                runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        system.forEach { t ->
            ResultLine("Ajuste · ${t.label}", lineColor) {
                runCatching { context.startActivity(android.content.Intent(t.action).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        if (query.isNotBlank() && results.isEmpty() && math == null && contacts.isEmpty() && system.isEmpty()) {
            Text("Nenhum app com “$query”", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        // conta: mostra o resultado; tocar copia
        math?.let { r ->
            Text(
                "= $r",
                color = lineColor,
                fontSize = 30.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                modifier = Modifier
                    .clickable {
                        context.getSystemService(android.content.ClipboardManager::class.java)
                            .setPrimaryClip(android.content.ClipData.newPlainText("conta", r))
                        android.widget.Toast.makeText(context, "Copiado", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 8.dp),
            )
        }
        // buscar na web com o que foi digitado
        if (query.isNotBlank() && math == null) {
            Text(
                "Buscar “${query.trim()}” na web",
                color = lineColor.copy(alpha = .8f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { searchWeb(context, query.trim()) }
                    .padding(vertical = 10.dp),
            )
        }
        SearchField(
            value = query,
            onValueChange = { query = it },
            color = lineColor,
            onGo = { results.firstOrNull()?.let(onLaunch) },
            modifier = Modifier
                .padding(top = 8.dp)
                .focusRequester(focus),
        )
    }
}

@Composable
private fun ResultLine(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        color = color,
        fontSize = 15.sp,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}

/** Até 3 contatos que batem com o texto; sem permissão ou sem texto, nada. */
private suspend fun searchContacts(context: android.content.Context, query: String): List<Pair<String, android.net.Uri>> {
    if (query.trim().length < 2) return emptyList()
    if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return emptyList()
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val c = android.provider.ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(query.trim()).build()
            val cols = arrayOf(android.provider.ContactsContract.Contacts._ID, android.provider.ContactsContract.Contacts.LOOKUP_KEY, android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            context.contentResolver.query(c, cols, null, null, null)?.use { r ->
                buildList {
                    while (r.moveToNext() && size < 3) {
                        add(r.getString(2).orEmpty() to android.provider.ContactsContract.Contacts.getLookupUri(r.getLong(0), r.getString(1)))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}

/** Pílula translúcida no estilo do cartão de música: lupa, texto na cor do traço e "×" para limpar. */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(TextStyle(color = color, fontSize = 17.sp)),
        cursorBrush = SolidColor(color),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            SearchPillFrame(
                color = color,
                showPlaceholder = value.isEmpty(),
                onClear = if (value.isNotEmpty()) ({ onValueChange("") }) else null,
                inner = inner,
            )
        },
    )
}

/** A mesma pílula, só para tocar e abrir o buscador (barra fixa no fim do modo grade). */
@Composable
fun SearchPillButton(color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SearchPillFrame(
        color = color,
        showPlaceholder = true,
        onClear = null,
        onClick = onClick,
        modifier = modifier,
    ) {}
}

@Composable
private fun SearchPillFrame(
    color: Color,
    showPlaceholder: Boolean,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    inner: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val lw = LocalLineScale.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Buscar app", onClick = onClick) else Modifier)
            .background(Color.White.copy(alpha = 0.08f))
            .border(LocalBorderWidth.current, color.copy(alpha = 0.3f), shape)
            .padding(start = 18.dp, end = 6.dp),
    ) {
        Canvas(Modifier.size(18.dp)) {
            val w = size.width
            val stroke = w * 0.1f * lw
            val c = color.copy(alpha = 0.7f)
            drawCircle(c, radius = w * 0.33f, center = Offset(w * 0.42f, w * 0.42f), style = Stroke(stroke))
            drawLine(c, Offset(w * 0.67f, w * 0.67f), Offset(w * 0.92f, w * 0.92f), stroke, StrokeCap.Round)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (showPlaceholder) Text("Buscar app", color = color.copy(alpha = 0.45f), fontSize = 17.sp)
            inner()
        }
        if (onClear != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
            ) {
                Canvas(Modifier.size(12.dp)) {
                    val w = size.width
                    val c = color.copy(alpha = 0.7f)
                    drawLine(c, Offset(0f, 0f), Offset(w, w), w * 0.14f * lw, StrokeCap.Round)
                    drawLine(c, Offset(w, 0f), Offset(0f, w), w * 0.14f * lw, StrokeCap.Round)
                }
            }
        }
    }
}

/** Abre a busca do Google no navegador padrão. */
private fun searchWeb(context: android.content.Context, q: String) {
    // link direto: vai para o navegador padrão, sem o seletor de apps do Android
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=" + android.net.Uri.encode(q)))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
