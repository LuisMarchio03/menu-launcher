package com.brendon978.menulauncher.home

import android.graphics.Typeface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.settings.FontChoice
import com.brendon978.menulauncher.settings.LauncherSettings

/** Medidas da tela inicial já com os multiplicadores dos ajustes (1 = o original). */
val LauncherSettings.rowIconSize: Dp get() = 24.dp * iconScale
val LauncherSettings.rowTextSize: TextUnit get() = 15.sp * textScale
/** A linha nunca fica mais baixa que o ícone com folga. */
val LauncherSettings.rowHeight: Dp get() = max(42.dp * rowScale, rowIconSize + 12.dp)
val LauncherSettings.gridIconSize: Dp get() = 28.dp * iconScale
/** Espaço do ícone na célula da grade (também a moldura da pasta). */
val LauncherSettings.gridCellSize: Dp get() = gridIconSize + 4.dp
val LauncherSettings.gridTextSize: TextUnit get() = 11.sp * textScale
val LauncherSettings.smallTextSize: TextUnit get() = 12.sp * textScale

/** O que uma linha de app precisa, em valores estáveis: a linha só recompõe quando um deles muda. */
@Immutable
data class RowLook(val color: Int, val iconSize: Dp, val textSize: TextUnit, val height: Dp, val stroke: Float, val shadow: Boolean)

val LauncherSettings.rowLook: RowLook
    get() = RowLook(strokeColor, rowIconSize, rowTextSize, rowHeight, strokeScale, textShadow)

private val CONDENSED = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

val FontChoice.family: FontFamily
    get() = when (this) {
        FontChoice.SYSTEM -> FontFamily.Default
        FontChoice.CONDENSED -> CONDENSED
        FontChoice.SERIF -> FontFamily.Serif
        FontChoice.MONO -> FontFamily.Monospace
    }

/** Sombra escura atrás do texto, para ler sobre papel de parede claro. */
private val TEXT_SHADOW = Shadow(Color.Black.copy(alpha = 0.65f), Offset(0f, 2f), blurRadius = 6f)

/** Fonte e sombra escolhidas, para todo Text dentro de [content]. */
@Composable
fun ProvideLauncherText(settings: LauncherSettings, content: @Composable () -> Unit) {
    val base = LocalTextStyle.current
    val style = remember(base, settings.font, settings.textShadow) {
        base.merge(TextStyle(fontFamily = settings.font.family, shadow = if (settings.textShadow) TEXT_SHADOW else null))
    }
    ProvideTextStyle(style, content)
}
