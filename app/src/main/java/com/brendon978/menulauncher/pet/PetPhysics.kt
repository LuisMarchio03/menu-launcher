package com.brendon978.menulauncher.pet

import kotlin.math.abs
import kotlin.math.sign

/**
 * Quantas vezes o dedo mudou de direção (no eixo x) andando ao menos [minTravel] para o outro lado.
 * Duas ou mais em pouco tempo = carinho. Tremida pequena não conta.
 */
fun countReversals(xs: List<Float>, minTravel: Float): Int {
    if (xs.isEmpty()) return 0
    var dir = 0f
    var extreme = xs.first()
    var reversals = 0
    for (x in xs) {
        val d = x - extreme
        when {
            dir == 0f -> if (abs(d) >= minTravel) { dir = sign(d); extreme = x }
            d * dir > 0f -> extreme = x
            abs(d) >= minTravel -> { reversals++; dir = -dir; extreme = x }
        }
    }
    return reversals
}

/** Corpo do Glitch voando (canto superior esquerdo e velocidade, em px e px/s). */
data class Body(val x: Float, val y: Float, val vx: Float, val vy: Float)

const val GRAVITY = 2_600f        // px/s²
private const val BOUNCE = .45f   // quanto do impulso volta no quique
private const val WALL = .5f
private const val STOP_VY = 60f   // abaixo disso não quica mais
private const val GROUND_FRICTION = .85f

/**
 * Um passo da física: gravidade, paredes da [area] e pouso no chão ou em cima de um apoio ([supports],
 * como a lista ou o cartão de música). Só pousa vindo de cima. Devolve o corpo e se já assentou.
 */
fun physicsStep(b: Body, dt: Float, area: Area, supports: List<Area>, w: Float, h: Float): Pair<Body, Boolean> {
    var vx = b.vx
    var vy = b.vy + GRAVITY * dt
    var x = b.x + vx * dt
    var y = b.y + vy * dt

    if (x < area.l) { x = area.l; vx = -vx * WALL }
    if (x + w > area.r) { x = area.r - w; vx = -vx * WALL }
    if (y < area.t) { y = area.t; vy = -vy * WALL }

    // chão: a base da área ou o topo de um apoio que ele cruzou descendo
    var grounded = false
    if (vy >= 0f) {
        val wasBottom = b.y + h
        val floors = supports
            .filter { s -> x < s.r && x + w > s.l && wasBottom <= s.t + .5f && y + h >= s.t }
            .map { it.t } + area.b
        val floor = floors.filter { y + h >= it }.minOrNull()
        if (floor != null) {
            y = floor - h
            vy = if (vy < STOP_VY) 0f else -vy * BOUNCE
            grounded = vy == 0f
        }
    }
    if (grounded) vx *= GROUND_FRICTION
    val settled = grounded && abs(vx) < 20f
    if (settled) vx = 0f
    return Body(x, y, vx, vy) to settled
}
