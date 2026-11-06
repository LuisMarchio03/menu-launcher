package com.brendon978.menulauncher.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** O Glitch de outra pessoa de visita. Hoje vem de teste; com o servidor, vem do amigo. */
data class Visitor(val name: String, val color: Color, val accessory: Accessory = Accessory.NONE, val buddy: Buddy = Buddy.NONE)

enum class VisitScene(val ms: Long) {
    ARRIVE(3_000), HIGH_FIVE(3_500), DANCE(6_000), CHASE(6_000), JOKENPO(6_000), BALL(7_000), SHOW_OFF(5_000), ARGUE(5_000), PHOTO(4_000), GOODBYE(3_000),
}

private val MIDDLE = listOf(VisitScene.HIGH_FIVE, VisitScene.DANCE, VisitScene.CHASE, VisitScene.JOKENPO, VisitScene.BALL, VisitScene.SHOW_OFF, VisitScene.ARGUE, VisitScene.PHOTO)

/** Sempre chega e se despede; no meio, de 3 a 4 cenas sem repetir. */
fun visitPlan(random: Random): List<VisitScene> =
    listOf(VisitScene.ARRIVE) + MIDDLE.shuffled(random).take(3 + random.nextInt(2)) + VisitScene.GOODBYE

/** Falas de cada cena: primeiro o seu Glitch, depois o visitante. */
fun visitLines(scene: VisitScene, name: String, random: Random): Pair<String, String> = when (scene) {
    VisitScene.ARRIVE -> listOf("E aí, $name!" to "Cheguei! Bonita sua tela.", "Olha quem apareceu!" to "Vim ver se você tá vivo.").random(random)
    VisitScene.HIGH_FIVE -> "Toca aqui!" to "Toca!"
    VisitScene.DANCE -> listOf("Solta o som!" to "Olha esse passo!", "Dança comigo!" to "Sou melhor que você!").random(random)
    VisitScene.CHASE -> "Tá com você!" to "Volta aqui, mané!"
    VisitScene.JOKENPO -> "Jokenpô valendo a tela!" to "Fechado!"
    VisitScene.BALL -> "Chuta pra mim!" to "Lá vai!"
    VisitScene.SHOW_OFF -> listOf("Gostou do meu visual?" to "O meu é melhor.", "Olha meu acessório." to "Bonitinho. Pra iniciante.").random(random)
    VisitScene.ARGUE -> listOf("Meu dono é mais legal." to "Teu dono nem me dá carinho!", "Sua bateria tá fraca." to "Tua cara que tá!").random(random)
    VisitScene.PHOTO -> "Foto!" to "Pega meu lado bom!"
    VisitScene.GOODBYE -> listOf("Volta logo!" to "Fui! Cuida dessa tela.", "Valeu a visita!" to "Até mais, $name na área!").random(random)
}

/** Quem ganha o jokenpô da visita: as mãos e o resultado saem do mesmo sorteio. */
fun visitHands(random: Random): Pair<Hand, Hand> = Hand.entries.random(random) to Hand.entries.random(random)

private fun Hand.face() = when (this) {
    Hand.ROCK -> Face.ROCK
    Hand.PAPER -> Face.PAPER
    Hand.SCISSORS -> Face.SCISSORS
}

/** Pose do seu Glitch e do visitante na cena. [guestLeft] diz se o visitante olha para a esquerda. */
fun visitPoses(scene: VisitScene, k: Float, t: Float, guestLeft: Boolean, hands: Pair<Hand, Hand>): Pair<RobotPose, RobotPose> {
    val hostLeft = !guestLeft
    val walk = sin(t * 12f)
    val s = sin(t * 7f)
    val calm = RobotPose(Face.NORMAL, hostLeft, phase = t) to RobotPose(Face.NORMAL, guestLeft, phase = t)
    return when (scene) {
        VisitScene.ARRIVE -> RobotPose(Face.GIGGLE, hostLeft, armL = 8f, armR = -125f + 16f * s, phase = t) to
            (if (k < .6f) RobotPose(Face.GIGGLE, guestLeft, legL = 18f * walk, legR = -18f * walk, armL = -16f * walk, armR = 16f * walk, phase = t)
            else RobotPose(Face.GIGGLE, guestLeft, armL = 125f - 16f * s, armR = -8f, phase = t))
        VisitScene.HIGH_FIVE -> {
            val up = if (k < .5f) -125f else -95f
            RobotPose(Face.BLISS, hostLeft, armL = 8f, armR = up, lift = if (k in .45f..6f) 6f else 0f, phase = t) to
                RobotPose(Face.BLISS, guestLeft, armL = up * -1f, armR = -8f, lift = if (k in .45f..6f) 6f else 0f, phase = t)
        }
        VisitScene.DANCE -> RobotPose(Face.MUSIC, hostLeft, armL = 40f + 30f * s, armR = -40f + 30f * s, tilt = 7f * s, lift = 4f * abs(s), phase = t) to
            RobotPose(Face.MUSIC, guestLeft, armL = 40f - 30f * s, armR = -40f - 30f * s, tilt = -7f * s, lift = 4f * abs(s), phase = t)
        VisitScene.CHASE -> RobotPose(Face.GIGGLE, hostLeft, legL = 22f * walk, legR = -22f * walk, armL = -24f * walk, armR = 24f * walk, phase = t) to
            RobotPose(Face.ANGRY, guestLeft, legL = 22f * walk, legR = -22f * walk, armL = -24f * walk, armR = 24f * walk, phase = t)
        VisitScene.JOKENPO -> if (k < .45f) {
            val bob = 30f * abs(sin(t * 9f))
            RobotPose(Face.SERIOUS, hostLeft, armL = 8f, armR = -60f - bob, phase = t) to RobotPose(Face.SERIOUS, guestLeft, armL = 60f + bob, armR = -8f, phase = t)
        } else RobotPose(hands.first.face(), hostLeft, armL = 8f, armR = -60f, phase = t) to RobotPose(hands.second.face(), guestLeft, armL = 60f, armR = -8f, phase = t)
        VisitScene.BALL -> RobotPose(Face.GIGGLE, hostLeft, legL = if (ballSide(k)) 26f * abs(s) else 0f, armL = 20f, armR = -20f, phase = t) to
            RobotPose(Face.GIGGLE, guestLeft, legL = if (ballSide(k)) 0f else 26f * abs(s), armL = 20f, armR = -20f, phase = t)
        VisitScene.SHOW_OFF -> RobotPose(Face.BLISS, hostLeft, armL = 110f, armR = -110f, tilt = 5f * s, phase = t) to
            RobotPose(Face.SERIOUS, guestLeft, armL = 70f, armR = -70f, headTilt = 8f, phase = t)
        VisitScene.ARGUE -> RobotPose(Face.ANGRY, hostLeft, armL = 25f, armR = -105f + 22f * s, phase = t) to
            RobotPose(Face.ANGRY, guestLeft, armL = 105f - 22f * s, armR = -25f, phase = t)
        VisitScene.PHOTO -> RobotPose(Face.BLISS, hostLeft, armL = 125f, armR = -40f, phase = t) to RobotPose(Face.GIGGLE, guestLeft, armL = 40f, armR = -125f, phase = t)
        VisitScene.GOODBYE -> if (k < .5f) RobotPose(Face.SAD, hostLeft, armL = 8f, armR = -125f + 16f * s, phase = t) to RobotPose(Face.GIGGLE, guestLeft, armL = 125f - 16f * s, armR = -8f, phase = t)
        else calm.first to RobotPose(Face.GIGGLE, !guestLeft, legL = 18f * walk, legR = -18f * walk, phase = t)
    }
}

/** A bola troca de lado a cada quarto da cena: true é a vez do seu Glitch chutar. */
fun ballSide(k: Float): Boolean = (k * 4f).toInt() % 2 == 0

/** Bola em arco entre os dois. */
fun ballAt(k: Float, host: Offset, guest: Offset, height: Float): Offset {
    val f = (k * 4f) % 1f
    val from = if (ballSide(k)) host else guest
    val to = if (ballSide(k)) guest else host
    return from + (to - from) * f + Offset(0f, -sin(f * PI.toFloat()) * height * 1.1f)
}

/** Lugar do visitante: entra pela borda, fica ao lado e sai de novo. */
fun guestPlace(scene: VisitScene, k: Float, standing: Offset, edge: Offset): Offset = when (scene) {
    VisitScene.ARRIVE -> edge + (standing - edge) * (k / .6f).coerceAtMost(1f)
    VisitScene.GOODBYE -> if (k < .5f) standing else standing + (edge - standing) * ((k - .5f) / .5f)
    else -> standing
}
