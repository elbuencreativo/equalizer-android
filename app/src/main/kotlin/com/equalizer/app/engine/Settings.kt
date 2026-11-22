package com.equalizer.app.engine

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.equalizer.app.core.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

class Settings(private val store: DataStore<Preferences>) {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val presetKey = stringPreferencesKey("preset")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val balanceKey = floatPreferencesKey("balance")
    private val methodKey = stringPreferencesKey("method")
    private val offsetsKey = stringPreferencesKey("offsets")

    val enabled: Flow<Boolean> = store.data.map { it[enabledKey] ?: true }
    val preset: Flow<Preset> = store.data.map { p ->
        Preset.values().firstOrNull { it.name == p[presetKey] } ?: Preset.FLAT
    }

    val method: Flow<EqMethod> = store.data.map { p ->
        EqMethod.values().firstOrNull { it.name == p[methodKey] } ?: EqMethod.GLOBAL
    }

    val onboardingDone: Flow<Boolean> = store.data.map { it[onboardingKey] ?: false }

    suspend fun setOnboardingDone(done: Boolean) { store.edit { it[onboardingKey] = done } }

    val balance: Flow<Float> = store.data.map { it[balanceKey] ?: 0f }

    suspend fun setBalance(v: Float) { store.edit { it[balanceKey] = v.coerceIn(-1f, 1f) } }

    suspend fun setMethod(m: EqMethod) { store.edit { it[methodKey] = m.name } }

    suspend fun setEnabled(on: Boolean) { store.edit { it[enabledKey] = on } }
    val offsets: Flow<List<Float>> = store.data.map { p ->
        p[offsetsKey]?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 10 } ?: List(10) { 0f }
    }

    /** Elegir un preset reinicia los ajustes manuales de banda. */
    suspend fun setPreset(p: Preset) {
        store.edit {
            it[presetKey] = p.name
            it.remove(offsetsKey)
        }
    }

    suspend fun setOffsets(values: List<Float>) { store.edit { it[offsetsKey] = values.joinToString(",") } }
}
