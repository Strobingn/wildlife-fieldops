package com.strobingn.wildlifefieldops.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.strobingn.wildlifefieldops.BuildConfig
import com.strobingn.wildlifefieldops.data.local.AppDatabase
import com.strobingn.wildlifefieldops.data.preferences.AppSettingsKeys
import com.strobingn.wildlifefieldops.data.preferences.settingsDataStore
import com.strobingn.wildlifefieldops.data.remote.SupabaseService
import com.strobingn.wildlifefieldops.data.remote.WeatherService
import com.strobingn.wildlifefieldops.data.repository.SyncRepository
import com.strobingn.wildlifefieldops.ui.theme.ThemeController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncRepository: SyncRepository,
    private val supabaseService: SupabaseService,
    private val weatherService: WeatherService,
    private val database: AppDatabase
) : ViewModel() {

    // Shared singleton DataStore — see AppSettings.kt
    private val dataStore = context.settingsDataStore

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val connectionStatus: StateFlow<String> = flow {
        emit(buildConnectionStatus())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Checking…")

    private val settings = dataStore.data
        .catch { emit(emptyPreferences()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyPreferences())

    val darkTheme = settings.map { it[AppSettingsKeys.DARK_THEME] ?: true }
    val notificationsEnabled = settings.map { it[AppSettingsKeys.NOTIFICATIONS_ENABLED] ?: true }
    val autoSync = settings.map { it[AppSettingsKeys.AUTO_SYNC] ?: true }
    val syncInterval = settings.map { it[AppSettingsKeys.SYNC_INTERVAL] ?: 15 }
    val companyName = settings.map { it[AppSettingsKeys.COMPANY_NAME] ?: "Wildlife Whisperer LLC" }
    val technicianName = settings.map { it[AppSettingsKeys.TECHNICIAN_NAME] ?: "" }
    val defaultTaxRate = settings.map { it[AppSettingsKeys.DEFAULT_TAX_RATE] ?: 0f }
    val offlineMode = settings.map { it[AppSettingsKeys.OFFLINE_MODE] ?: false }
    val highAccuracyGps = settings.map { it[AppSettingsKeys.HIGH_ACCURACY_GPS] ?: true }

    init {
        // Keep in-memory theme controller in sync with persisted preference.
        viewModelScope.launch {
            darkTheme.collect { enabled ->
                ThemeController.setDark(enabled)
            }
        }
    }

    private fun buildConnectionStatus(): String {
        val cloud = if (supabaseService.isConfigured) "Supabase OK" else "Supabase missing"
        val maps = if (
            BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank() &&
            !BuildConfig.GOOGLE_MAPS_API_KEY.contains("YOUR_")
        ) "Maps OK" else "Maps missing"
        val weather = if (weatherService.isConfigured) "Weather OK" else "Weather optional"
        return "$cloud · $maps · $weather"
    }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        ThemeController.setDark(enabled)
        dataStore.edit { it[AppSettingsKeys.DARK_THEME] = enabled }
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    fun setAutoSync(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.AUTO_SYNC] = enabled }
    }

    fun setSyncInterval(minutes: Int) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.SYNC_INTERVAL] = minutes }
    }

    fun setCompanyName(name: String) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.COMPANY_NAME] = name }
    }

    fun setTechnicianName(name: String) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.TECHNICIAN_NAME] = name }
    }

    fun setDefaultTaxRate(rate: Float) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.DEFAULT_TAX_RATE] = rate }
    }

    fun setOfflineMode(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.OFFLINE_MODE] = enabled }
    }

    fun setHighAccuracyGps(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AppSettingsKeys.HIGH_ACCURACY_GPS] = enabled }
    }

    fun triggerManualSync() = viewModelScope.launch {
        if (_isSyncing.value) return@launch
        _isSyncing.value = true
        _syncMessage.value = "Syncing…"
        try {
            val offline = offlineMode.first()
            if (offline) {
                _syncMessage.value = "Offline mode is on. Turn it off to sync."
                return@launch
            }
            if (!syncRepository.isCloudConfigured()) {
                _syncMessage.value =
                    "Cloud not configured. Rebuild APK with Supabase secrets set (Settings shows connection status)."
                return@launch
            }
            val result = syncRepository.syncAll()
            _syncMessage.value = result.message
        } catch (t: Throwable) {
            android.util.Log.e("SettingsViewModel", "Sync UI crash prevented", t)
            _syncMessage.value = "Sync error: ${t.message ?: t.javaClass.simpleName}"
        } finally {
            _isSyncing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun exportData() = viewModelScope.launch {
        _syncMessage.value = "Export: use Share from job/invoice PDFs for now. Full dump coming in a later update."
    }

    fun importData() = viewModelScope.launch {
        _syncMessage.value = "Import: use Sync Now to pull jobs and customers from Supabase."
    }

    fun clearAllData() = viewModelScope.launch {
        try {
            // clearAllTables is NOT suspend — must leave main thread or Room crashes.
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            _syncMessage.value = "All local data cleared."
        } catch (t: Throwable) {
            android.util.Log.e("SettingsViewModel", "Clear data failed", t)
            _syncMessage.value = "Clear failed: ${t.message ?: t.javaClass.simpleName}"
        }
    }
}
