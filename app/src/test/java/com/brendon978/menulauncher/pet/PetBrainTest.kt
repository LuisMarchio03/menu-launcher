package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetBrainTest {
    private fun mood(hour: Int = 12, battery: Int? = 80, charging: Boolean = false, music: Boolean = false, happyUntil: Long = 0, now: Long = 1000) =
        baseMood(hour, battery, charging, music, happyUntil, now)

    @Test fun dormeDas23As7() {
        assertEquals(Mood.SLEEPING, mood(hour = 23))
        assertEquals(Mood.SLEEPING, mood(hour = 3))
        assertEquals(Mood.NORMAL, mood(hour = 7))
        assertEquals(Mood.NORMAL, mood(hour = 22))
    }

    @Test fun cansadoComBateriaBaixaSemCarregar() {
        assertEquals(Mood.TIRED, mood(battery = 15))
        assertEquals(Mood.NORMAL, mood(battery = 16))
        assertEquals(Mood.NORMAL, mood(battery = 10, charging = true))
        assertEquals(Mood.NORMAL, mood(battery = null))
    }

    @Test fun prioridadeDormindoCansadoMusicaFeliz() {
        assertEquals(Mood.SLEEPING, mood(hour = 2, battery = 5, music = true))
        assertEquals(Mood.TIRED, mood(battery = 5, music = true))
        assertEquals(Mood.DANCING, mood(music = true, happyUntil = 5000))
        assertEquals(Mood.HAPPY, mood(happyUntil = 5000, now = 1000))
        assertEquals(Mood.NORMAL, mood(happyUntil = 5000, now = 5000))
    }

    @Test fun felizSoQuandoNotificacoesZeram() {
        assertEquals(61_000L, happyUntil(prev = 3, new = 0, now = 1_000, current = 0))
        assertEquals(0L, happyUntil(prev = 0, new = 0, now = 1_000, current = 0))
        assertEquals(0L, happyUntil(prev = null, new = 0, now = 1_000, current = 0))
        assertEquals(500L, happyUntil(prev = 2, new = 1, now = 1_000, current = 500))
    }

    @Test fun toqueDeVezEmQuandoESempreRisadinha() {
        var a = Annoyance()
        repeat(5) { i ->
            val (next, r) = a.tap(now = i * 10_000L)
            assertEquals(Reaction.GIGGLE, r)
            a = next
        }
    }

    @Test fun importunarSobeAteFugirEDepoisFicaSerio() {
        var a = Annoyance()
        var afterFlee: Annoyance? = null
        val reactions = (0 until 8).map { i ->
            val (next, r) = a.tap(now = i * 200L)
            if (r == Reaction.FLEE && afterFlee == null) afterFlee = next
            a = next
            r
        }
        assertEquals(Reaction.GIGGLE, reactions.first())
        assertTrue(Reaction.SERIOUS in reactions)
        assertTrue(Reaction.ANGRY in reactions)
        val flee = reactions.indexOf(Reaction.FLEE)
        assertTrue("deve fugir", flee > 0)
        assertTrue(reactions.indexOf(Reaction.ANGRY) < flee)
        val menace = reactions.indexOf(Reaction.MENACE)
        assertTrue("aponta a arma antes de fugir", menace in (reactions.indexOf(Reaction.ANGRY) + 1) until flee)
        // reaparece ainda apontando a arma
        assertEquals(Reaction.MENACE, afterFlee!!.levelReaction(afterFlee!!.at))
    }

    @Test fun ameacaContinuaEnquantoSeAcalma() {
        val a = Annoyance(level = 5f, at = 0)
        assertEquals(Reaction.MENACE, a.levelReaction(0))
        assertEquals(Reaction.ANGRY, a.levelReaction(2_000))   // 4.0
        assertEquals(Reaction.SERIOUS, a.levelReaction(4_000)) // 3.0
    }

    @Test fun marchaDepoisQueGuardaAMira() {
        // estava ameaçando e parou: marcha por MARCH_MS
        assertEquals(1_000L + MARCH_MS, marchUntil(wasMenacing = true, menacing = false, now = 1_000, current = 0))
        // ainda ameaçando ou nunca ameaçou: não muda
        assertEquals(0L, marchUntil(wasMenacing = true, menacing = true, now = 1_000, current = 0))
        assertEquals(500L, marchUntil(wasMenacing = false, menacing = false, now = 1_000, current = 500))
    }

    @Test fun risadinhaNaoFala() {
        assertEquals(null, angerLine(Reaction.GIGGLE, Random(1)))
    }

    @Test fun cadaNivelDeRaivaTemSuaFala() {
        val r = Random(5)
        for (level in listOf(Reaction.SERIOUS, Reaction.ANGRY, Reaction.MENACE, Reaction.FLEE)) {
            val line = angerLine(level, r)!!
            assertTrue(line in ANGER_LINES.getValue(level))
        }
        assertTrue(MARCH_LINES.isNotEmpty())
    }

    @Test fun passoDeDancaNuncaRepeteSeguido() {
        val r = Random(9)
        var cur = DanceMove.SWAY
        val seen = mutableSetOf(cur)
        repeat(500) {
            val next = nextDanceMove(r, cur)
            assertTrue(next != cur)
            seen += next
            cur = next
        }
        assertEquals(DanceMove.entries.toSet(), seen)
    }

    @Test fun irritacaoBaixaSozinha() {
        var a = Annoyance()
        repeat(4) { i -> a = a.tap(i * 200L).first }
        assertTrue(a.levelAt(600) > 2f)
        assertEquals(0f, a.levelAt(60_000), 0.001f)
    }

    // ---- tela toda ----
    private val area = Area(0f, 0f, 400f, 800f)
    private val top = Area(0f, 0f, 400f, 250f)       // relógio + música
    private val list = Area(0f, 500f, 330f, 800f)    // favoritos
    private val bar = Area(330f, 400f, 400f, 800f)   // letras

    @Test fun destinoLivreNaoEncostaEmNada() {
        val r = Random(7)
        repeat(300) {
            val p = freeSpot(r, area, 50f, 70f, listOf(top, list, bar), from = null, minDistance = 0f)!!
            val me = Area(p.first, p.second, p.first + 50f, p.second + 70f)
            assertTrue("dentro da tela", me.l >= 0f && me.t >= 0f && me.r <= 400f && me.b <= 800f)
            assertTrue("fora dos obstáculos", listOf(top, list, bar).none { it.intersects(me) })
        }
    }

    @Test fun semEspacoNaoTemDestino() {
        assertEquals(null, freeSpot(Random(1), area, 50f, 70f, listOf(area), from = null, minDistance = 0f))
    }

    @Test fun caminhoNaoAtravessaObstaculo() {
        // de cima-esquerda para baixo-esquerda passando pela lista: bloqueado
        assertEquals(false, pathClear(10f to 260f, 10f to 700f, 50f, 70f, listOf(list)))
        // na mesma faixa livre: liberado
        assertEquals(true, pathClear(10f to 300f, 250f to 380f, 50f, 70f, listOf(top, list, bar)))
    }

    @Test fun destinoComCaminhoLivreAPartirDeOndeEsta() {
        val r = Random(3)
        repeat(100) {
            val from = 20f to 300f
            val p = freeSpot(r, area, 50f, 70f, listOf(top, list, bar), from = from, minDistance = 40f)!!
            assertTrue(pathClear(from, p, 50f, 70f, listOf(top, list, bar)))
        }
    }
}
