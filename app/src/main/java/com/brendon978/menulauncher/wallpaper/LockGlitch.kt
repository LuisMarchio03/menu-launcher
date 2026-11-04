package com.brendon978.menulauncher.wallpaper

import com.brendon978.menulauncher.pet.Face
import com.brendon978.menulauncher.pet.Mood
import com.brendon978.menulauncher.pet.RobotPose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** O que o Glitch faz de vez em quando na tela de bloqueio: quietinho, uma coisa curta por vez. */
enum class LockAct(val durationMs: Long) { WAVE(1_800), LOOK(2_400), SCAN(2_200), STRETCH(2_000), HOP(1_200) }

private const val WAKE_ACT_CHANCE = .4f

/** O bloqueio fica à vista poucos segundos: conta a partir de a tela acender. null = hoje fica só parado. */
fun wakeActDelay(random: Random): Long? =
    if (random.nextFloat() < WAKE_ACT_CHANCE) random.nextLong(1_500, 4_500) else null

/** Se a tela continuar acesa, a próxima coisa demora bem mais. */
fun nextActGap(random: Random): Long = random.nextLong(20_000, 45_000)

fun pickAct(previous: LockAct?, random: Random): LockAct = LockAct.entries.filter { it != previous }.random(random)

/** Só faz algo quando está de boa: dormindo, cansado ou dançando já tem o que fazer. */
fun actsIn(mood: Mood): Boolean = mood == Mood.NORMAL || mood == Mood.HAPPY

/** Parado ele quase não redesenha, para gastar pouca bateria. */
fun lockFrameDelay(act: LockAct?, mood: Mood): Long = when {
    act != null -> 40L
    mood == Mood.DANCING -> 66L
    mood == Mood.SLEEPING -> 300L
    else -> 200L
}

/** [k] é o avanço da ação (0 a 1); [t] é o relógio em segundos. */
fun lockPose(act: LockAct?, k: Float, mood: Mood, t: Float): RobotPose {
    val wave = sin(k * 2f * PI.toFloat())
    return when {
        mood == Mood.SLEEPING -> RobotPose(Face.SLEEP, armL = 8f, armR = -8f, phase = t)
        mood == Mood.TIRED -> RobotPose(Face.TIRED, armL = 8f, armR = -8f, tilt = 4f, phase = t)
        mood == Mood.DANCING -> {
            val s = sin(t * 6f)
            RobotPose(Face.MUSIC, armL = -20f * s, armR = -20f * s, tilt = 4f * s, lift = 2f * abs(s), phase = t)
        }
        act == LockAct.WAVE -> RobotPose(armL = 6f, armR = -118f + 16f * sin(k * 6f * PI.toFloat()), phase = t)
        act == LockAct.LOOK -> RobotPose(lookX = wave, phase = t)
        act == LockAct.SCAN -> RobotPose(Face.SCAN, antennaOn = (t * 6).toInt() % 2 == 0, phase = t)
        act == LockAct.STRETCH -> RobotPose(armL = 122f * abs(wave), armR = -122f * abs(wave), lift = 3f * abs(wave), phase = t) // positivo abre o braço esquerdo para fora
        act == LockAct.HOP -> RobotPose(Face.GIGGLE, armL = -30f, armR = 30f, lift = 9f * abs(wave), phase = t)
        else -> RobotPose(blink = if (t % 4.5f < .28f) .1f else 1f, phase = t)
    }
}
