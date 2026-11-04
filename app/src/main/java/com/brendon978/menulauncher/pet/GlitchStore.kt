package com.brendon978.menulauncher.pet

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

private val Context.glitchStore by preferencesDataStore(name = "glitch_brain")

/** O que o Glitch lembra entre um uso e outro: vontades, vínculo e quando te viu pela última vez. */
data class GlitchMemory(
    val drives: Drives = Drives(),
    val bond: Float = .5f,
    val lastSeen: Long = 0L,
    val pokeExpect: Float = 0f,
    val diary: Diary = Diary(0),
    val lastMorning: Int = 0,
    val lastNightNag: Long = 0L,
    /** Melhor tempo no duelo, em ms; 0 = nenhum ainda. */
    val duelBest: Long = 0L,
)

class GlitchStore(private val context: Context) {
    private object K {
        val energy = floatPreferencesKey("energy")
        val boredom = floatPreferencesKey("boredom")
        val longing = floatPreferencesKey("longing")
        val curiosity = floatPreferencesKey("curiosity")
        val bond = floatPreferencesKey("bond")
        val lastSeen = longPreferencesKey("last_seen")
        val pokeExpect = floatPreferencesKey("poke_expect")
        val diaryDay = intPreferencesKey("diary_day")
        val thrown = intPreferencesKey("diary_thrown")
        val petted = intPreferencesKey("diary_petted")
        val poked = intPreferencesKey("diary_poked")
        val games = intPreferencesKey("diary_games")
        val fled = intPreferencesKey("diary_fled")
        val lastMorning = intPreferencesKey("last_morning")
        val lastNightNag = longPreferencesKey("last_night_nag")
        val duelBest = longPreferencesKey("duel_best")
    }

    suspend fun load(): GlitchMemory {
        val p = context.glitchStore.data.first()
        val d = Drives()
        return GlitchMemory(
            drives = Drives(p[K.energy] ?: d.energy, p[K.boredom] ?: d.boredom, p[K.longing] ?: d.longing, p[K.curiosity] ?: d.curiosity).clamped(),
            bond = (p[K.bond] ?: .5f).coerceIn(0f, 1f),
            lastSeen = p[K.lastSeen] ?: 0L,
            pokeExpect = p[K.pokeExpect] ?: 0f,
            diary = Diary(p[K.diaryDay] ?: 0, p[K.thrown] ?: 0, p[K.petted] ?: 0, p[K.poked] ?: 0, p[K.games] ?: 0, p[K.fled] ?: 0),
            lastMorning = p[K.lastMorning] ?: 0,
            lastNightNag = p[K.lastNightNag] ?: 0L,
            duelBest = p[K.duelBest] ?: 0L,
        )
    }

    suspend fun save(m: GlitchMemory) {
        context.glitchStore.edit {
            it[K.energy] = m.drives.energy
            it[K.boredom] = m.drives.boredom
            it[K.longing] = m.drives.longing
            it[K.curiosity] = m.drives.curiosity
            it[K.bond] = m.bond
            it[K.lastSeen] = m.lastSeen
            it[K.pokeExpect] = m.pokeExpect
            it[K.diaryDay] = m.diary.day
            it[K.thrown] = m.diary.thrown
            it[K.petted] = m.diary.petted
            it[K.poked] = m.diary.poked
            it[K.games] = m.diary.games
            it[K.fled] = m.diary.fled
            it[K.lastMorning] = m.lastMorning
            it[K.lastNightNag] = m.lastNightNag
            it[K.duelBest] = m.duelBest
        }
    }
}

/** Retrato do cérebro agora, para o painel dos Ajustes. */
data class GlitchSnapshot(val drives: Drives, val bond: Float, val activity: Activity, val irritation: Float, val doing: String, val diary: String = "")

/** A memória do Glitch como chaves "glitch.*" do arquivo de backup. */
object GlitchBackup {
    fun toExtras(m: GlitchMemory): Map<String, String> = mapOf(
        "glitch.energy" to m.drives.energy.toString(), "glitch.boredom" to m.drives.boredom.toString(),
        "glitch.longing" to m.drives.longing.toString(), "glitch.curiosity" to m.drives.curiosity.toString(),
        "glitch.bond" to m.bond.toString(), "glitch.pokeExpect" to m.pokeExpect.toString(),
        "glitch.duelBest" to m.duelBest.toString(),
    )

    fun fromExtras(x: Map<String, String>): GlitchMemory? {
        val bond = x["glitch.bond"]?.toFloatOrNull() ?: return null
        val d = Drives()
        fun f(k: String, def: Float) = x[k]?.toFloatOrNull() ?: def
        return GlitchMemory(
            drives = Drives(f("glitch.energy", d.energy), f("glitch.boredom", d.boredom), f("glitch.longing", d.longing), f("glitch.curiosity", d.curiosity)).clamped(),
            bond = bond.coerceIn(0f, 1f),
            lastSeen = System.currentTimeMillis(),
            pokeExpect = f("glitch.pokeExpect", 0f),
            duelBest = x["glitch.duelBest"]?.toLongOrNull() ?: 0L,
        )
    }
}

object GlitchLive {
    /** Importando backup: o Glitch que está na tela não pode salvar por cima. */
    @Volatile var importing = false
    private val _state = MutableStateFlow<GlitchSnapshot?>(null)
    val state: StateFlow<GlitchSnapshot?> = _state.asStateFlow()
    fun publish(s: GlitchSnapshot) { _state.value = s }
}
