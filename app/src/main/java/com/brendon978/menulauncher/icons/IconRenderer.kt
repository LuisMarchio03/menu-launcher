package com.brendon978.menulauncher.icons

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.brendon978.menulauncher.settings.InitialStyle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import kotlin.math.roundToInt

/**
 * Desenha os ícones de contorno. Tudo sai numa cor só (a do ajuste), nunca com as cores do logo.
 * [iconOf] devolve o ícone do sistema do app (para a camada monocromática).
 */
class IconRenderer(
    private val context: Context,
    filterLoader: () -> AppFilter,
    private val iconOf: (AppKey) -> Drawable?,
) {
    /** Carregado na primeira vez que um ícone é desenhado (sempre fora da thread principal). */
    val filter: AppFilter by lazy(filterLoader)

    private data class CacheKey(val source: IconSource, val sizePx: Int, val color: Int, val stroke: Float, val initial: InitialStyle)

    /** Forma do ícone de letra (app sem desenho no pacote). Estado do Compose: quem desenha relê ao trocar. */
    var initialStyle by mutableStateOf(InitialStyle.SQUIRCLE)

    private val cache = LruCache<CacheKey, ImageBitmap>(400)
    // Os ícones são desenhados em várias threads ao mesmo tempo (um por linha visível).
    private val monoCache = Memo<AppKey, Drawable?> { key -> (iconOf(key) as? AdaptiveIconDrawable)?.monochrome }

    /** [stroke] multiplica a espessura do traço (1 = a do Lawnicons). */
    fun iconFor(entry: AppEntry, override: String?, sizePx: Int, color: Int, stroke: Float = 1f): ImageBitmap {
        val source = resolveIcon(entry.key, entry.label, override, filter) { monochromeOf(it) != null }
        return render(source, sizePx, color, stroke)
    }

    fun render(source: IconSource, sizePx: Int, color: Int, stroke: Float = 1f): ImageBitmap {
        val style = initialStyle
        val key = CacheKey(source, sizePx, color, stroke, style)
        cache.get(key)?.let { return it }
        val bmp = when (source) {
            is IconSource.Lawnicon -> renderSvg("lawnicons/svgs/${source.name}.svg", sizePx, color, stroke)
                ?: renderInitial('?', sizePx, color, stroke, style)
            is IconSource.Own -> renderSvg("proprios/${source.name}.svg", sizePx, color, stroke)
                ?: renderInitial('?', sizePx, color, stroke, style)
            is IconSource.Monochrome -> monochromeOf(source.key)?.let { renderMonochrome(it, sizePx, color, stroke) }
                ?: renderInitial('?', sizePx, color, stroke, style)
            is IconSource.Initial -> renderInitial(source.letter, sizePx, color, stroke, style)
        }
        return bmp.asImageBitmap().also { cache.put(key, it) }
    }

    fun clear() {
        cache.evictAll()
        monoCache.clear()
    }

    private fun monochromeOf(key: AppKey): Drawable? = monoCache.get(key)

    private fun renderSvg(path: String, sizePx: Int, color: Int, stroke: Float): Bitmap? {
        val svg = runCatching {
            if (stroke == 1f) SVG.getFromAsset(context.assets, path)
            else SVG.getFromString(scaleStrokeWidths(context.assets.open(path).bufferedReader().use { it.readText() }, stroke))
        }.getOrNull() ?: return null
        if (svg.documentViewBox == null) {
            svg.setDocumentViewBox(0f, 0f, svg.documentWidth.coerceAtLeast(1f), svg.documentHeight.coerceAtLeast(1f))
        }
        // Lawnicons traz width/height="192": sem isso o AndroidSVG desenha em 192px e corta o ícone.
        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")
        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        svg.renderToCanvas(canvas, RenderOptions().viewPort(0f, 0f, sizePx.toFloat(), sizePx.toFloat()))
        // Pinta traço e preenchimento (alguns ícones têm pontos cheios) na cor escolhida.
        canvas.drawColor(color, PorterDuff.Mode.SRC_IN)
        return bmp
    }

    private fun renderMonochrome(mono: Drawable, sizePx: Int, color: Int, stroke: Float): Bitmap {
        // A camada adaptativa tem 108dp com 72dp visíveis: sobra 1/4 de cada lado.
        val bmp = createBitmap(TRACE_SIZE, TRACE_SIZE)
        val extra = TRACE_SIZE / 4
        // O mesmo Drawable pode ser pedido em dois tamanhos ao mesmo tempo: bounds+draw não podem se cruzar.
        synchronized(mono) {
            mono.setBounds(-extra, -extra, TRACE_SIZE + extra, TRACE_SIZE + extra)
            mono.draw(Canvas(bmp))
        }
        val pixels = IntArray(TRACE_SIZE * TRACE_SIZE)
        bmp.getPixels(pixels, 0, TRACE_SIZE, 0, 0, TRACE_SIZE, TRACE_SIZE)
        val alpha = IntArray(pixels.size) { Color.alpha(pixels[it]) }
        val edge = OutlineTracer.trace(alpha, TRACE_SIZE, (STROKE * stroke).roundToInt().coerceAtLeast(2))
        val out = IntArray(pixels.size) { if (edge[it]) color else Color.TRANSPARENT }
        bmp.setPixels(out, 0, TRACE_SIZE, 0, 0, TRACE_SIZE, TRACE_SIZE)
        return if (sizePx == TRACE_SIZE) bmp else bmp.scale(sizePx, sizePx)
    }

    private fun renderInitial(letter: Char, sizePx: Int, color: Int, strokeScale: Float, style: InitialStyle): Bitmap {
        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        val stroke = sizePx * STROKE * strokeScale / TRACE_SIZE.toFloat()
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeJoin = Paint.Join.ROUND
            this.color = color
        }
        val c = sizePx / 2f
        val inset = stroke
        when (style) {
            InitialStyle.CIRCLE -> canvas.drawCircle(c, c, c - stroke, ring)
            // quadrado arredondado: a mesma moldura dos desenhos do Lawnicons
            InitialStyle.SQUIRCLE -> canvas.drawRoundRect(inset, inset, sizePx - inset, sizePx - inset, sizePx * .3f, sizePx * .3f, ring)
            InitialStyle.LETTER -> Unit
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            // sozinha a letra cresce e fica fina, para pesar como um desenho
            textSize = sizePx * if (style == InitialStyle.LETTER) 0.78f else 0.45f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (style == InitialStyle.LETTER) 300 else 500, false)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(letter.toString(), c, c - (text.descent() + text.ascent()) / 2, text)
        return bmp
    }

    private companion object {
        const val TRACE_SIZE = 192
        const val STROKE = 14
    }
}
