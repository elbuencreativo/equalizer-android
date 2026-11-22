package com.equalizer.app.device

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.equalizer.app.core.DeviceType
import kotlinx.coroutines.flow.first

val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore("devices")

class DeviceRegistry(private val store: DataStore<Preferences>) {
    private fun keyOf(key: String) = stringPreferencesKey("k:$key")
    private fun nameOf(name: String) = stringPreferencesKey("n:$name")

    suspend fun assign(key: String, name: String, type: DeviceType) {
        store.edit {
            it[keyOf(key)] = type.name
            it[nameOf(name)] = type.name
        }
    }

    suspend fun typeFor(key: String, name: String): DeviceType? {
        val prefs = store.data.first()
        val raw = prefs[keyOf(key)] ?: prefs[nameOf(name)] ?: return null
        return DeviceType.values().firstOrNull { it.name == raw }
    }
}
