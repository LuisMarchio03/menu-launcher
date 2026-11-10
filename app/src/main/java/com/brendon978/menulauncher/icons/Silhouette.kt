package com.brendon978.menulauncher.icons

/**
 * Silhueta do desenho de um ícone colorido (ARGB, sem pré-multiplicar), para app sem camada monocromática.
 * Forma solta sobre fundo transparente é o próprio desenho. Placa (quadrado, círculo ou tela cheia) não
 * diz nada pelo alfa: vale o que destoa da cor de fundo dela. null quando não sai um desenho limpo
 * (foto, degradê forte, placa lisa, quase nada): aí fica a letra.
 */
object Silhouette {
    /** Abaixo disso o desenho é um pontinho. */
    private const val MIN_COVER = 0.015
    /** Forma que enche a própria caixa a partir disso é placa (o círculo enche ~78%). */
    private const val PLATE_FILL = 0.74
    /** Distância de cor (RGB, 0 a 441) que separa o símbolo do fundo da placa. */
    private const val COLOR_GAP = 60
    /** O fundo da placa tem de ser uma cor só em boa parte dela; o símbolo, no máximo metade. */
    private const val MIN_BACKGROUND = 0.45
    private const val MAX_SYMBOL = 0.5
    /** Pedaço menor que isso é cisco (sai); mais pedaços que [MAX_PARTS] é texto ou ilustração (fica a letra). */
    private const val MIN_PART = 0.003
    private const val MAX_PARTS = 6

    fun extract(argb: IntArray, size: Int): BooleanArray? {
        val n = size * size
        val solid = BooleanArray(n) { (argb[it] ushr 24) >= 128 }
        val count = solid.count { it }
        if (count < n * MIN_COVER) return null
        var x0 = size; var y0 = size; var x1 = -1; var y1 = -1
        for (i in 0 until n) if (solid[i]) {
            val x = i % size; val y = i / size
            if (x < x0) x0 = x; if (x > x1) x1 = x
            if (y < y0) y0 = y; if (y > y1) y1 = y
        }
        val box = (x1 - x0 + 1) * (y1 - y0 + 1)
        val shape = if (count < box * PLATE_FILL) solid else symbolOnPlate(argb, size, shrink(solid, size, (size / 32).coerceAtLeast(1)))
        return shape?.let { tidy(it, size) }
    }

    /** Tira os ciscos; null se sobrar nada ou pedaço demais para um desenho limpo. */
    private fun tidy(mask: BooleanArray, size: Int): BooleanArray? {
        val out = BooleanArray(mask.size)
        val seen = BooleanArray(mask.size)
        val stack = IntArray(mask.size)
        val part = IntArray(mask.size)
        var parts = 0
        for (start in mask.indices) {
            if (!mask[start] || seen[start]) continue
            var top = 0; var len = 0
            stack[top++] = start; seen[start] = true
            while (top > 0) {
                val i = stack[--top]
                part[len++] = i
                val x = i % size
                if (x > 0 && mask[i - 1] && !seen[i - 1]) { seen[i - 1] = true; stack[top++] = i - 1 }
                if (x < size - 1 && mask[i + 1] && !seen[i + 1]) { seen[i + 1] = true; stack[top++] = i + 1 }
                if (i >= size && mask[i - size] && !seen[i - size]) { seen[i - size] = true; stack[top++] = i - size }
                if (i + size < mask.size && mask[i + size] && !seen[i + size]) { seen[i + size] = true; stack[top++] = i + size }
            }
            if (len < mask.size * MIN_PART) continue
            if (++parts > MAX_PARTS) return null
            for (k in 0 until len) out[part[k]] = true
        }
        return if (parts == 0) null else out
    }

    private fun symbolOnPlate(argb: IntArray, size: Int, inner: BooleanArray): BooleanArray? {
        val total = inner.count { it }
        if (total == 0) return null
        // Cor de fundo: a mais comum (em 16 níveis por canal), pela média dos pixels dela.
        val bins = IntArray(4096)
        for (i in inner.indices) if (inner[i]) bins[bin(argb[i])]++
        val top = bins.indices.maxBy { bins[it] }
        var r = 0L; var g = 0L; var b = 0L
        for (i in inner.indices) if (inner[i] && bin(argb[i]) == top) {
            r += argb[i] shr 16 and 0xFF; g += argb[i] shr 8 and 0xFF; b += argb[i] and 0xFF
        }
        val ref = intArrayOf((r / bins[top]).toInt(), (g / bins[top]).toInt(), (b / bins[top]).toInt())
        val out = BooleanArray(inner.size) { inner[it] && far(argb[it], ref) }
        val symbol = out.count { it }
        if (symbol < total * MIN_COVER || symbol > total * MAX_SYMBOL || total - symbol < total * MIN_BACKGROUND) return null
        return out
    }

    private fun bin(c: Int) = (c shr 20 and 0xF shl 8) or (c shr 12 and 0xF shl 4) or (c shr 4 and 0xF)

    private fun far(c: Int, ref: IntArray): Boolean {
        val dr = (c shr 16 and 0xFF) - ref[0]
        val dg = (c shr 8 and 0xFF) - ref[1]
        val db = (c and 0xFF) - ref[2]
        return dr * dr + dg * dg + db * db > COLOR_GAP * COLOR_GAP
    }

    /** Tira [steps] pixels da borda da placa: a beirada serrilhada misturada com o fundo não é símbolo. */
    private fun shrink(mask: BooleanArray, size: Int, steps: Int): BooleanArray {
        var cur = mask
        repeat(steps) {
            val prev = cur
            cur = BooleanArray(prev.size) { i ->
                val x = i % size; val y = i / size
                prev[i] && x > 0 && x < size - 1 && y > 0 && y < size - 1 &&
                    prev[i - 1] && prev[i + 1] && prev[i - size] && prev[i + size]
            }
        }
        return cur
    }
}
