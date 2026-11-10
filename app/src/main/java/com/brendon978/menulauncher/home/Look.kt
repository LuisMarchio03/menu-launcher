package com.brendon978.menulauncher.home

import android.graphics.Typeface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.brendon978.menulauncher.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.settings.FontChoice
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.lineScale
import com.brendon978.menulauncher.wallpaper.haloFor

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
data class RowLook(
    val color: Int,
    val iconSize: Dp,
    val textSize: TextUnit,
    val height: Dp,
    val stroke: Float,
    val shadow: Boolean,
    val light: Boolean = false,
    val motion: Boolean = false,
    val align: com.brendon978.menulauncher.settings.ListAlign = com.brendon978.menulauncher.settings.ListAlign.START,
    val label: Boolean = true,
)

val LauncherSettings.rowLook: RowLook
    get() = RowLook(strokeColor, rowIconSize, rowTextSize, rowHeight, strokeScale, textShadow, appWeight == com.brendon978.menulauncher.settings.AppWeight.LIGHT, motion, listAlign, showLabels)

private val CONDENSED = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

/** Fontes variáveis que vêm no app (licença OFL, em assets/licencas): um peso por instância do eixo wght. */
@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int) = FontFamily(
    listOf(FontWeight.Thin, FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { w ->
        Font(res, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    },
)

private val MANROPE = variable(R.font.manrope)
private val OUTFIT = variable(R.font.outfit)

val FontChoice.family: FontFamily
    get() = when (this) {
        FontChoice.SYSTEM -> FontFamily.Default
        FontChoice.CONDENSED -> CONDENSED
        FontChoice.SERIF -> FontFamily.Serif
        FontChoice.MONO -> FontFamily.Monospace
        FontChoice.MANROPE -> MANROPE
        FontChoice.OUTFIT -> OUTFIT
    }

/** Forma em volta dos ícones de app (contorno puro, círculo ou quadrado translúcido, placa). */
val LocalIconShape = compositionLocalOf { com.brendon978.menulauncher.settings.IconShape.OUTLINE }

/** Claridade da placa dos ícones, de 0 a 1 (ver plateColors). */
val LocalPlateLight = compositionLocalOf { 0.3f }

/** Borda fina dos contornos (1 dp no original), com a espessura dos ajustes. */
val LocalBorderWidth = compositionLocalOf { 1.dp }

/** Multiplicador do traço dos desenhos, com a espessura dos ajustes. */
val LocalLineScale = compositionLocalOf { 1f }

/** Sombra atrás do texto no tom oposto ao traço ([haloFor]), para ler sobre papel de parede claro ou com detalhe. */
private fun textShadow(stroke: Int) = Shadow(Color(haloFor(stroke)).copy(alpha = 0.65f), Offset(0f, 2f), blurRadius = 6f)

/** Fonte, sombra e espessura das bordas escolhidas, para tudo dentro de [content]. */
@Composable
fun ProvideLauncherText(settings: LauncherSettings, content: @Composable () -> Unit) {
    val base = LocalTextStyle.current
    val style = remember(base, settings.font, settings.textShadow, settings.strokeColor) {
        base.merge(TextStyle(fontFamily = settings.font.family, shadow = if (settings.textShadow) textShadow(settings.strokeColor) else null))
    }
    CompositionLocalProvider(
        LocalIconShape provides settings.iconShape,
        LocalPlateLight provides settings.plateLight,
        LocalBorderWidth provides 1.dp * settings.borderScale,
        LocalLineScale provides lineScale(settings.borderScale),
    ) { ProvideTextStyle(style, content) }
}
