package com.brendon978.menulauncher.icons

/**
 * Converte uma silhueta (canal alfa) em contorno: borda = dilate(mask, r) && !erode(mask, r),
 * com r = thickness/2 e vizinhança em disco (cantos redondos, como o traço dos Lawnicons).
 */
object OutlineTracer {
    /** Fração da área que sobra ao erodir abaixo da qual o desenho é de traço fino. */
    private const val THIN = 0.25

    fun trace(alpha: IntArray, size: Int, thickness: Int): BooleanArray =
        trace(BooleanArray(size * size) { alpha[it] >= 128 }, size, thickness)

    fun trace(mask: BooleanArray, size: Int, thickness: Int): BooleanArray {
        val r = thickness / 2
        val eroded = morph(mask, size, r, dilate = false)
        // Desenho já feito de traço fino (some quase todo ao erodir): contornar grudaria os dois lados
        // de cada linha num borrão, então ele fica como está. Um ponto solto ainda vira disco.
        val area = mask.count { it }
        if (area >= thickness * thickness && eroded.count { it } < area * THIN) return mask.copyOf()
        val dilated = morph(mask, size, r, dilate = true)
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
