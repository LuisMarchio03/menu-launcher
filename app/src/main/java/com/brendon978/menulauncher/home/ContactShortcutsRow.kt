package com.brendon978.menulauncher.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.pet.petObstacle
import com.brendon978.menulauncher.settings.ContactShortcut
import com.brendon978.menulauncher.settings.contactInitials

/** Até 4 contatos como iniciais em contorno; tocar abre o contato. */
@Composable
fun ContactShortcutsRow(contacts: List<ContactShortcut>, color: Color) {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 10.dp).petObstacle("contatos"),
    ) {
        contacts.forEach { contact ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(LocalBorderWidth.current, color.copy(alpha = .4f), CircleShape)
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(contact.uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }.onFailure { Toast.makeText(context, "Não consegui abrir esse contato", Toast.LENGTH_SHORT).show() }
                    },
            ) {
                Text(contactInitials(contact.name), color = color, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}
