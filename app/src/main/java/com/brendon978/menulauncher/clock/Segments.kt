package com.brendon978.menulauncher.clock

/** Segmentos acesos do dígito, um bit cada: a=topo, b, c (direita), d=base, e, f (esquerda), g=meio. 0 fora dos dígitos. */
fun segmentsOf(c: Char): Int = if (c in '0'..'9') DIGITS[c - '0'] else 0

private val DIGITS = intArrayOf(0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F)
