package com.equalizer.app.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.customPresetsDataStore: DataStore<Preferences> by preferencesDataStore("custom_presets")

data class CustomPreset(val name: String, val curve: EqCurve)

private const val RECORD = '\u001E'
private const val FIELD = '\u001F'

class CustomPresets(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("list")

    val all: Flow<List<CustomPreset>> = store.data.map { decode(it[key].orEmpty()) }

    suspend fun save(p: CustomPreset) {
        val name = p.name.trim()
        require(name.isNotEmpty()) { "El nombre del preset no puede estar vacío" }
        store.edit { prefs ->
            val rest = decode(prefs[key].orEmpty()).filter { it.name != name }
            prefs[key] = encode(rest + CustomPreset(name, p.curve))
        }
    }

    suspend fun delete(name: String) {
        store.edit { prefs -> prefs[key] = encode(decode(prefs[key].orEmpty()).filter { it.name != name }) }
    }

    private fun encode(list: List<CustomPreset>) =
        list.joinToString(RECORD.toString()) { "${it.name}$FIELD${it.curve.gainsDb.joinToString(",")}" }

    private fun decode(raw: String): List<CustomPreset> =
        raw.split(RECORD).filter { it.isNotEmpty() }.mapNotNull { rec ->
            val parts = rec.split(FIELD)
            if (parts.size != 2) return@mapNotNull null
            val gains = parts[1].split(",").mapNotNull { it.toFloatOrNull() }
            if (gains.size != BAND_FREQS_HZ.size) null else CustomPreset(parts[0], EqCurve(gains))
        }
}
