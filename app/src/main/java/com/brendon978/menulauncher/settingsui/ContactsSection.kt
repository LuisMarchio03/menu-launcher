package com.brendon978.menulauncher.settingsui

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.settings.ContactOps
import com.brendon978.menulauncher.settings.ContactShortcut
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.MAX_CONTACTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Atalhos de contatos: o seletor do sistema entrega só o contato escolhido, sem pedir acesso aos contatos. */
@Composable
fun ContactsSection(settings: LauncherSettings, onChange: ((LauncherSettings) -> LauncherSettings) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = withContext(Dispatchers.IO) { displayName(context, uri) }
            onChange { it.copy(contactShortcuts = ContactOps.add(it.contactShortcuts, ContactShortcut(uri.toString(), name.orEmpty()))) }
        }
    }
    ToggleRow("Atalhos de contatos", "Até $MAX_CONTACTS contatos como iniciais na tela inicial; tocar abre o contato.", settings.showContacts) { v ->
        onChange { it.copy(showContacts = v) }
    }
    if (!settings.showContacts) return
    settings.contactShortcuts.forEach { contact ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(contact.name.ifBlank { "Sem nome" }, color = Color.White, modifier = Modifier.weight(1f))
            Text(
                "✕",
                color = Color.White.copy(alpha = .9f),
                modifier = Modifier
                    .clickable { onChange { s -> s.copy(contactShortcuts = s.contactShortcuts.filter { it.uri != contact.uri }) } }
                    .padding(14.dp),
            )
        }
    }
    if (settings.contactShortcuts.size < MAX_CONTACTS) {
        Text(
            "Adicionar contato",
            color = Color.White,
            modifier = Modifier.fillMaxWidth().clickable { pick.launch(null) }.padding(vertical = 12.dp),
        )
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
