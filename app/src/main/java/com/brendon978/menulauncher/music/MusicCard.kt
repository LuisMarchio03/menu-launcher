package com.brendon978.menulauncher.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Topo: só mostra o que está tocando. Os controles ficam embaixo (MusicControls). */
@Composable
fun MusicCard(state: MusicState, color: Color, modifier: Modifier = Modifier, textSize: TextUnit = 12.sp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        val art = remember(state.art) { state.art?.asImageBitmap() }
        if (art != null) {
            Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
        } else {
            Box(Modifier.size(36.dp).border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp)))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(state.title, color = color, fontSize = textSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(state.artist, state.appLabel).joinToString(" · "),
                color = color.copy(alpha = 0.6f),
                fontSize = textSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
