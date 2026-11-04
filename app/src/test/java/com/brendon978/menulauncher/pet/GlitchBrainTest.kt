package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GlitchBrainTest {
    private val calm = Drives(energy = .8f, boredom = .1f, longing = .1f, curiosity = .1f)

    // ---- vontades com o tempo ----
    @Test fun acordadoGastaEnergiaECochilandoRecarrega() {
        val awake = tick(calm, 600f, TickCtx(napping = false))
        assertTrue(awake.energy < calm.energy)
        val nap = tick(awake, 120f, TickCtx(napping = true))
        assertTrue(nap.energy > awake.energy)
    }

    @Test fun noiteEBateriaBaixaCansamMaisRapido() {
        val normal = tick(calm, 600f, TickCtx())
        val night = tick(calm, 600f, TickCtx(night = true))
        val low = tick(calm, 600f, TickCtx(batteryLow = true))
        assertTrue(night.energy < normal.energy)
        assertTrue(low.energy < normal.energy)
    }

    @Test fun semNadaAcontecerTedioECarenciaSobem() {
        val later = tick(calm, 900f, TickCtx())
        assertTrue(later.boredom > calm.boredom)
        assertTrue(later.longing > calm.longing)
    }

    @Test fun musicaTiraOTedio() {
        val bored = calm.copy(boredom = .8f)
        assertTrue(tick(bored, 120f, TickCtx(music = true)).boredom < tick(bored, 120f, TickCtx()).boredom)
    }

    @Test fun curiosidadePassa() {
        assertTrue(tick(calm.copy(curiosity = .9f), 300f, TickCtx()).curiosity < .9f)
    }

    @Test fun vontadesFicamEntreZeroEUm() {
        val d = tick(Drives(0f, 1f, 1f, 0f), 100_000f, TickCtx(night = true, batteryLow = true))
        for (v in listOf(d.energy, d.boredom, d.longing, d.curiosity)) assertTrue(v in 0f..1f)
    }

    // ---- eventos ----
    @Test fun toqueGentilMataTedioECarencia() {
        val d = onEvent(calm.copy(boredom = .9f, longing = .9f), GlitchEvent.GENTLE_TAP)
        assertTrue(d.boredom < .6f && d.longing < .5f)
    }

    @Test fun notificacaoDeixaCurioso() {
        assertTrue(onEvent(calm, GlitchEvent.NOTIFICATION).curiosity > .5f)
    }

    // ---- decisão ----
    private fun decideMany(d: Drives, bond: Float = .5f, music: Boolean = false) =
        (0 until 300).map { decide(d, bond, music, Random(it)) }.groupingBy { it }.eachCount()

    @Test fun semEnergiaCochila() {
        val c = decideMany(calm.copy(energy = .05f))
        assertEquals(Activity.NAP, c.maxBy { it.value }.key)
    }

    @Test fun entediadoChamaAtencao() {
        val c = decideMany(calm.copy(boredom = .95f, longing = .9f))
        assertEquals(Activity.SEEK_ATTENTION, c.maxBy { it.value }.key)
    }

    @Test fun curiosoExplora() {
        val c = decideMany(calm.copy(curiosity = .95f))
        assertEquals(Activity.EXPLORE, c.maxBy { it.value }.key)
    }

    @Test fun musicaComVinculoBomDanca() {
        assertEquals(Activity.DANCE, decideMany(calm, bond = .7f, music = true).maxBy { it.value }.key)
    }

    @Test fun vinculoRuimEmburraERecusaDanca() {
        val c = decideMany(calm, bond = .05f, music = true)
        assertEquals(Activity.SULK, c.maxBy { it.value }.key)
        assertEquals(null, c[Activity.DANCE])
    }

    @Test fun calmoPasseia() {
        assertEquals(Activity.WANDER, decideMany(calm).maxBy { it.value }.key)
    }

    // ---- vínculo ----
    @Test fun vinculoSobeComCarinhoEDesceComRaiva() {
        assertTrue(bondAfter(.5f, Reaction.GIGGLE) > .5f)
        assertTrue(bondAfter(.5f, Reaction.ANGRY) < .5f)
        assertTrue(bondAfter(.5f, Reaction.FLEE) < bondAfter(.5f, Reaction.ANGRY))
        assertEquals(1f, bondAfter(1f, Reaction.GIGGLE), .0001f)
        assertEquals(0f, bondAfter(0f, Reaction.FLEE), .0001f)
    }

    @Test fun vinculoAltoDemoraMaisAFicarBravo() {
        fun tapsUntilAngry(bond: Float): Int {
            var a = Annoyance()
            for (i in 1..30) {
                val (n, r) = a.tap(i * 200L, bond)
                a = n
                if (r == Reaction.ANGRY || r == Reaction.MENACE || r == Reaction.FLEE) return i
            }
            return 99
        }
        assertTrue(tapsUntilAngry(.95f) > tapsUntilAngry(.5f))
        assertTrue(tapsUntilAngry(.05f) < tapsUntilAngry(.5f))
    }

    // ---- voltar depois de sumir ----
    @Test fun voltaRapidoNaoMudaNada() {
        val (d, greet) = onReturn(calm, awayMs = 10 * 60_000L, bond = .5f)
        assertEquals(calm, d)
        assertEquals(null, greet)
    }

    @Test fun sumiuHorasDaSaudadeOuEmburra() {
        val (d, good) = onReturn(calm, awayMs = 5 * 3_600_000L, bond = .7f)
        assertTrue(d.longing > calm.longing)
        assertEquals(Greeting.MISSED_YOU, good)
        assertEquals(Greeting.SULKY, onReturn(calm, 5 * 3_600_000L, bond = .15f).second)
    }

    // ---- tristeza, brincar, fogos, protesto, castigo ----
    @Test fun carenteEEntediadoHaMuitoTempoFicaTriste() {
        val c = decideMany(calm.copy(longing = 1f, boredom = .9f), bond = .4f)
        assertEquals(Activity.SAD, c.maxBy { it.value }.key)
    }

    @Test fun entediadoComEnergiaEPoucoCarenteBrinca() {
        val c = decideMany(calm.copy(energy = .9f, boredom = .7f, longing = .1f))
        assertEquals(Activity.PLAY, c.maxBy { it.value }.key)
    }

    @Test fun cansadoNaoBrinca() {
        assertEquals(null, decideMany(calm.copy(energy = .35f, boredom = .7f, longing = .1f))[Activity.PLAY])
    }

    @Test fun fogosQuandoOVinculoChegaNoTopo() {
        assertTrue(celebrates(prevBond = .84f, newBond = .86f))
        assertEquals(false, celebrates(prevBond = .9f, newBond = .93f))
        assertEquals(false, celebrates(prevBond = .5f, newBond = .53f))
    }

    @Test fun protestaDepoisDeTresRaivasEmPoucosMinutos() {
        var m = ProtestMeter()
        assertEquals(false, m.angry(0).second)
        m = m.angry(0).first
        assertEquals(false, m.angry(60_000).second)
        m = m.angry(60_000).first
        assertTrue(m.angry(120_000).second)
        // raivas espalhadas (mais de 10 min) não somam
        var spread = ProtestMeter()
        var protested = false
        for (i in 0 until 5) { val (n, p) = spread.angry(i * 11 * 60_000L); spread = n; protested = protested || p }
        assertEquals(false, protested)
    }

    @Test fun castigoAcabaNoTempoOuNoCarinho() {
        val start = Punishment.start(now = 0)
        assertTrue(start.active(10_000))
        assertEquals(false, start.active(PUNISH_MS + 1))
        assertEquals(false, start.forgiven().active(10_000))
    }

    @Test fun importunarNoCastigoFazFugir() {
        var p = Punishment.start(now = 0)
        var fled = false
        for (i in 1..6) { val (n, f) = p.poke(i * 300L); p = n; fled = fled || f }
        assertTrue(fled)
    }

    @Test fun tristezaDuraAteReceberAtencao() {
        val carente = Drives(energy = .6f, boredom = 1f, longing = 1f, curiosity = .2f)
        repeat(50) { assertEquals(Activity.SAD, nextActivity(Activity.SAD, carente, .5f, false, Random(it))) }
        val atendido = onEvent(carente, GlitchEvent.GENTLE_TAP).clamped()
        assertTrue((0 until 50).any { nextActivity(Activity.SAD, atendido, .5f, false, Random(it)) != Activity.SAD })
    }

    @Test fun bemCarenteFicaTristeMaisDoQuePedeAtencao() {
        val carente = Drives(energy = .6f, boredom = 1f, longing = 1f, curiosity = .2f)
        val c = (0 until 300).map { decide(carente, .5f, false, Random(it)) }.groupingBy { it }.eachCount()
        assertTrue((c[Activity.SAD] ?: 0) > (c[Activity.SEEK_ATTENTION] ?: 0))
    }
}
