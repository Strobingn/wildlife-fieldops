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
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showDiagnostics by remember { mutableStateOf(false) }
    var showAiOperations by remember { mutableStateOf(false) }
    if (showDiagnostics) {
        DiagnosticsScreen(onBack = { showDiagnostics = false })
        return
    }
    if (showAiOperations) {
        AIOperationsScreen(onBack = { showAiOperations = false })
        return
    }

    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val autoSync by viewModel.autoSync.collectAsState(initial = true)
    val offlineMode by viewModel.offlineMode.collectAsState(initial = false)
    val highAccuracyGps by viewModel.highAccuracyGps.collectAsState(initial = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingSection("Connection") {
                SettingItem(Icons.Default.Cloud, "Service Status", connectionStatus, showChevron = false)
                SettingItem(
                    Icons.Default.CloudSync,
                    if (isSyncing) "Syncing…" else "Sync Now",
                    if (offlineMode) "Offline mode is enabled" else "Push and pull Supabase data",
                    enabled = !isSyncing,
                    onClick = viewModel::triggerManualSync
                )
                SettingSwitch(Icons.Default.Sync, "Automatic Sync", "Keep local and cloud records synchronized", autoSync, viewModel::setAutoSync)
                SettingSwitch(Icons.Default.CloudOff, "Offline Mode", "Disable cloud requests", offlineMode, viewModel::setOfflineMode)
            }

            SettingSection("AI Operations") {
                SettingItem(
                    Icons.Default.AutoAwesome,
                    "AI Operations Command Center",
                    "Property intelligence, pricing, quality control, routing, inventory and species guidance",
                    onClick = { showAiOperations = true }
                )
            }

            SettingSection("Location") {
                SettingSwitch(Icons.Default.MyLocation, "High Accuracy GPS", "Use precise location for field work", highAccuracyGps, viewModel::setHighAccuracyGps)
            }

            SettingSection("Diagnostics") {
                SettingItem(
                    Icons.Default.BugReport,
                    "Developer Diagnostics",
                    "Test configuration, network, device and full cloud sync",
                    onClick = { showDiagnostics = true }
                )
            }

            SettingSection("Data") {
                SettingItem(Icons.Default.UploadFile, "Export Data", "Export available job and invoice documents", onClick = viewModel::exportData)
                SettingItem(Icons.Default.Download, "Import Data", "Pull records from Supabase", onClick = viewModel::importData)
            }

            SettingSection("App Info") {
                SettingItem(Icons.Default.Info, "Wildlife FieldOps", "Version ${BuildConfig.VERSION_NAME}", showChevron = false)
                SettingItem(Icons.Default.Storage, "Local Database", "Room (SQLite)", showChevron = false)
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = BackgroundCard), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier
    Row(
        modifier = Modifier.fillMaxWidth().then(clickModifier).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (enabled) PrimaryGreen else TextTertiary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (enabled) TextPrimary else TextTertiary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (showChevron && onClick != null) {
            if (title == "Syncing…") CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
