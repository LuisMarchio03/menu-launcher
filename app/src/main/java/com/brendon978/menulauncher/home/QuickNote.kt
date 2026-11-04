package com.brendon978.menulauncher.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.pet.petObstacle

const val NOTE_MAX = 200

/** Tira as sobras das pontas e corta no limite, sem partir um emoji no meio. */
fun cleanNote(text: String): String {
    val cut = text.trim().take(NOTE_MAX)
    return (if (cut.lastOrNull()?.isHighSurrogate() == true) cut.dropLast(1) else cut).trimEnd()
}

/** Nota curta na tela inicial; tocar edita. */
@Composable
fun QuickNote(text: String, color: Color, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Text(
        text.ifBlank { "Toque para anotar" },
        color = color.copy(alpha = if (text.isBlank()) .45f else .85f),
        fontSize = 13.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(top = 8.dp)
            .widthIn(max = 200.dp)
            .petObstacle("nota")
            .clickable(remember { MutableInteractionSource() }, indication = null) { editing = true },
    )
    if (editing) NoteDialog(text, onSave = { onSave(cleanNote(it)); editing = false }, onDismiss = { editing = false })
}

@Composable
private fun NoteDialog(text: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B),
        title = { Text("Nota", color = Color.White) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.copy(text = it.text.take(NOTE_MAX)) },
                minLines = 2,
                maxLines = 4,
                supportingText = { Text("${value.text.length}/$NOTE_MAX", color = Color.White.copy(alpha = .5f)) },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value.text) }) { Text("Salvar", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(alpha = .7f)) } },
    )
}
