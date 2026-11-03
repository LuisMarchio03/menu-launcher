package com.brendon978.menulauncher.pet

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

private val Context.glitchStore by preferencesDataStore(name = "glitch_brain")

/** O que o Glitch lembra entre um uso e outro: vontades, vínculo e quando te viu pela última vez. */
data class GlitchMemory(val drives: Drives = Drives(), val bond: Float = .5f, val lastSeen: Long = 0L)

class GlitchStore(private val context: Context) {
    private object K {
        val energy = floatPreferencesKey("energy")
        val boredom = floatPreferencesKey("boredom")
        val longing = floatPreferencesKey("longing")
        val curiosity = floatPreferencesKey("curiosity")
        val bond = floatPreferencesKey("bond")
        val lastSeen = longPreferencesKey("last_seen")
    }

    suspend fun load(): GlitchMemory {
        val p = context.glitchStore.data.first()
        val d = Drives()
        return GlitchMemory(
            drives = Drives(p[K.energy] ?: d.energy, p[K.boredom] ?: d.boredom, p[K.longing] ?: d.longing, p[K.curiosity] ?: d.curiosity).clamped(),
            bond = (p[K.bond] ?: .5f).coerceIn(0f, 1f),
            lastSeen = p[K.lastSeen] ?: 0L,
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
        }
    }
}

/** Retrato do cérebro agora, para o painel dos Ajustes. */
data class GlitchSnapshot(val drives: Drives, val bond: Float, val activity: Activity, val irritation: Float, val doing: String)

object GlitchLive {
    private val _state = MutableStateFlow<GlitchSnapshot?>(null)
    val state: StateFlow<GlitchSnapshot?> = _state.asStateFlow()
    fun publish(s: GlitchSnapshot) { _state.value = s }
}
