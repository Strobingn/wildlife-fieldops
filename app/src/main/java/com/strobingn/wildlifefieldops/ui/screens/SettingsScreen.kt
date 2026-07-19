package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.BuildConfig
import com.strobingn.wildlifefieldops.ui.theme.ThemeController
import com.strobingn.wildlifefieldops.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    var showDiagnostics by remember { mutableStateOf(false) }
    var showAiOperations by remember { mutableStateOf(false) }
    if (showDiagnostics) { DiagnosticsScreen(onBack = { showDiagnostics = false }); return }
    if (showAiOperations) { AIOperationsScreen(onBack = { showAiOperations = false }); return }

    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState(initial = ThemeController.isDark)
    val autoSync by viewModel.autoSync.collectAsState(initial = true)
    val offlineMode by viewModel.offlineMode.collectAsState(initial = false)
    val highAccuracyGps by viewModel.highAccuracyGps.collectAsState(initial = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSyncMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Configure field operations, appearance and cloud behavior.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            SettingSection("Appearance") {
                SettingSwitch(
                    Icons.Default.DarkMode,
                    "Dark theme",
                    "Switch between dark and light app appearance",
                    darkTheme,
                    viewModel::setDarkTheme
                )
            }

            SettingSection("Connection") {
                SettingItem(Icons.Default.Cloud, "Service status", connectionStatus, showChevron = false)
                SettingItem(Icons.Default.CloudSync, if (isSyncing) "Syncing…" else "Sync now", if (offlineMode) "Offline mode is enabled" else "Push and pull Supabase data", enabled = !isSyncing, onClick = viewModel::triggerManualSync)
                SettingSwitch(Icons.Default.Sync, "Automatic sync", "Keep local and cloud records synchronized", autoSync, viewModel::setAutoSync)
                SettingSwitch(Icons.Default.CloudOff, "Offline mode", "Disable cloud requests", offlineMode, viewModel::setOfflineMode)
            }

            SettingSection("Field operations") {
                SettingSwitch(Icons.Default.MyLocation, "High accuracy GPS", "Use precise location for inspections and routing", highAccuracyGps, viewModel::setHighAccuracyGps)
                SettingItem(Icons.Default.AutoAwesome, "AI operations", "Pricing, property intelligence, routing and species guidance", onClick = { showAiOperations = true })
            }

            SettingSection("Data and diagnostics") {
                SettingItem(Icons.Default.UploadFile, "Export data", "Export available job and invoice documents", onClick = viewModel::exportData)
                SettingItem(Icons.Default.Download, "Import data", "Pull records from Supabase", onClick = viewModel::importData)
                SettingItem(Icons.Default.BugReport, "Developer diagnostics", "Test configuration, network, device and cloud sync", onClick = { showDiagnostics = true })
            }

            SettingSection("App information") {
                SettingItem(Icons.Default.Info, "Wildlife Field App", "Version ${BuildConfig.VERSION_NAME}", showChevron = false)
                SettingItem(Icons.Default.Storage, "Local database", "Room / SQLite", showChevron = false)
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingItem(icon: ImageVector, title: String, subtitle: String, enabled: Boolean = true, showChevron: Boolean = true, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier
    Row(Modifier.fillMaxWidth().then(modifier).padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showChevron && onClick != null) Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingSwitch(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
