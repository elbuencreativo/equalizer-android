package com.equalizer.app.engine

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.equalizer.app.core.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

class Settings(private val store: DataStore<Preferences>) {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val presetKey = stringPreferencesKey("preset")

    val enabled: Flow<Boolean> = store.data.map { it[enabledKey] ?: true }
    val preset: Flow<Preset> = store.data.map { p ->
        Preset.values().firstOrNull { it.name == p[presetKey] } ?: Preset.FLAT
    }

    suspend fun setEnabled(on: Boolean) { store.edit { it[enabledKey] = on } }
    suspend fun setPreset(p: Preset) { store.edit { it[presetKey] = p.name } }
}
