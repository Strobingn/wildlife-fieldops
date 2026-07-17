package com.strobingn.wildlifefieldops.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(name = "settings")

object AppSettingsKeys {
    val AUTO_SYNC = booleanPreferencesKey("auto_sync")
    val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
    val SYNC_INTERVAL = intPreferencesKey("sync_interval")
}
