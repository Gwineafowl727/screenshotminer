package com.example.screenshotminer

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private object PreferencesKeys {
        val TARGET_FIELD = stringPreferencesKey("target_field")
        val MINING_TIMEOUT = longPreferencesKey("mining_timeout")
        val REDO_TIMEOUT = longPreferencesKey("redo_timeout")

        val IS_DELETE_ENABLED = booleanPreferencesKey("is_delete_enabled")
    }

    val targetField: Flow<String> = context.dataStore.data.map {
        prefs -> prefs[PreferencesKeys.TARGET_FIELD] ?: "Picture" // default value
    }

    val miningTimeout: Flow<Long> = context.dataStore.data.map {
        prefs -> prefs[PreferencesKeys.MINING_TIMEOUT] ?: 60000L // default value
    }

    val redoTimeout: Flow<Long> = context.dataStore.data.map {
        prefs -> prefs[PreferencesKeys.REDO_TIMEOUT] ?: 20000L // default value
    }

    val isDeleteEnabled: Flow<Boolean> = context.dataStore.data.map {
        prefs -> prefs[PreferencesKeys.IS_DELETE_ENABLED] ?: false // default value
    }

    suspend fun saveSettings(newFieldName: String, newMiningTimeout: Long, newRedoTimeout: Long, newIsDeleteEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TARGET_FIELD] = newFieldName
            prefs[PreferencesKeys.MINING_TIMEOUT] = newMiningTimeout
            prefs[PreferencesKeys.REDO_TIMEOUT] = newRedoTimeout
            prefs[PreferencesKeys.IS_DELETE_ENABLED] = newIsDeleteEnabled
        }
    }

}