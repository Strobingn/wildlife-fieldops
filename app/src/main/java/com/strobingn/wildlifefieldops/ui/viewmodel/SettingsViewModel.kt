package com.strobingn.wildlifefieldops.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val dataStore = context.dataStore

    companion object {
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

    val settings = dataStore.data
        .catch { emit(emptyPreferences()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyPreferences())

    val darkTheme = settings.map { it[DARK_THEME] ?: true }
    val notificationsEnabled = settings.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val autoSync = settings.map { it[AUTO_SYNC] ?: true }
    val syncInterval = settings.map { it[SYNC_INTERVAL] ?: 15 }
    val companyName = settings.map { it[COMPANY_NAME] ?: "Wildlife Whisperer LLC" }
    val technicianName = settings.map { it[TECHNICIAN_NAME] ?: "" }
    val defaultTaxRate = settings.map { it[DEFAULT_TAX_RATE] ?: 0f }
    val offlineMode = settings.map { it[OFFLINE_MODE] ?: false }
    val highAccuracyGps = settings.map { it[HIGH_ACCURACY_GPS] ?: true }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[DARK_THEME] = enabled }
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    fun setAutoSync(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AUTO_SYNC] = enabled }
    }

    fun setSyncInterval(minutes: Int) = viewModelScope.launch {
        dataStore.edit { it[SYNC_INTERVAL] = minutes }
    }

    fun setCompanyName(name: String) = viewModelScope.launch {
        dataStore.edit { it[COMPANY_NAME] = name }
    }

    fun setTechnicianName(name: String) = viewModelScope.launch {
        dataStore.edit { it[TECHNICIAN_NAME] = name }
    }

    fun setDefaultTaxRate(rate: Float) = viewModelScope.launch {
        dataStore.edit { it[DEFAULT_TAX_RATE] = rate }
    }

    fun setOfflineMode(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[OFFLINE_MODE] = enabled }
    }

    fun setHighAccuracyGps(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[HIGH_ACCURACY_GPS] = enabled }
    }

    fun triggerManualSync() = viewModelScope.launch {
        // Manual sync implementation would go here
    }

    fun exportData() = viewModelScope.launch {
        // Export data implementation
    }

    fun importData() = viewModelScope.launch {
        // Import data implementation
    }

    fun clearAllData() = viewModelScope.launch {
        // Clear all data implementation
    }
}
