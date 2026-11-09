package com.brendon978.menulauncher.icons

/**
 * Converte uma silhueta (canal alfa) em contorno: borda = dilate(mask, r) && !erode(mask, r),
 * com r = thickness/2 e vizinhança em disco (cantos redondos, como o traço dos Lawnicons).
 */
object OutlineTracer {
    fun trace(alpha: IntArray, size: Int, thickness: Int): BooleanArray {
        val mask = BooleanArray(size * size) { alpha[it] >= 128 }
        val r = thickness / 2
        val dilated = morph(mask, size, r, dilate = true)
        val eroded = morph(mask, size, r, dilate = false)
        return BooleanArray(size * size) { dilated[it] && !eroded[it] }
    }

    /**
     * Disco de raio [r]: cada linha dy do disco cobre x ± span[dy]. Com a soma acumulada de cada linha,
     * "tem algum cheio" (dilatar) ou "está tudo cheio" (erodir) no trecho sai em tempo constante.
     * Fora da imagem conta como vazio.
     */
    private fun morph(src: BooleanArray, size: Int, r: Int, dilate: Boolean): BooleanArray {
        val prefix = IntArray(size * (size + 1))
        for (y in 0 until size) {
            val row = y * (size + 1)
            for (x in 0 until size) prefix[row + x + 1] = prefix[row + x] + if (src[y * size + x]) 1 else 0
        }
        val span = IntArray(2 * r + 1) { i -> val dy = i - r; kotlin.math.sqrt((r * r - dy * dy).toDouble()).toInt() }
        val out = BooleanArray(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            var hit = !dilate
            for (i in span.indices) {
                val yy = y + i - r
                val from = x - span[i]
                val to = x + span[i]
                if (yy !in 0 until size) {
                    if (!dilate) { hit = false; break } else continue
                }
                if (!dilate && (from < 0 || to >= size)) { hit = false; break }
                val row = yy * (size + 1)
                val filled = prefix[row + minOf(size - 1, to) + 1] - prefix[row + maxOf(0, from)]
                if (dilate && filled > 0) { hit = true; break }
                if (!dilate && filled < to - from + 1) { hit = false; break }
            }
            out[y * size + x] = hit
        }
        return out
    }
}
