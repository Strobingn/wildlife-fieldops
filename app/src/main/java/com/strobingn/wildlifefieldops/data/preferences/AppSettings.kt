package com.strobingn.wildlifefieldops.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single shared DataStore for app settings.
 *
 * IMPORTANT: Do not declare another `preferencesDataStore(name = "settings")` elsewhere.
 * Multiple delegates for the same file crash at runtime with
 * "There are multiple DataStores active for the same file".
 */
val Context.settingsDataStore by preferencesDataStore(name = "settings")

object AppSettingsKeys {
    val DARK_THEME = booleanPreferencesKey("dark_theme")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val AUTO_SYNC = booleanPreferencesKey("auto_sync")
    val SYNC_INTERVAL = intPreferencesKey("sync_interval")
    val COMPANY_NAME = stringPreferencesKey("company_name")
    val TECHNICIAN_NAME = stringPreferencesKey("technician_name")
    val DEFAULT_TAX_RATE = floatPreferencesKey("default_tax_rate")
    val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
    val HIGH_ACCURACY_GPS = booleanPreferencesKey("high_accuracy_gps")
}
