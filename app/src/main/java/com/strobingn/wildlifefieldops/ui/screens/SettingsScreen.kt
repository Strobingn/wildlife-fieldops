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
    val scrollState = rememberScrollState()
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingSection(title = "Connection") {
                SettingItem(
                    icon = Icons.Default.Cloud,
                    title = "Service Status",
                    subtitle = connectionStatus,
                    showChevron = false
                )
                SettingItem(
                    icon = Icons.Default.CloudSync,
                    title = if (isSyncing) "Syncing…" else "Sync Now",
                    subtitle = if (offlineMode) "Offline mode is enabled" else "Push and pull Supabase data",
                    enabled = !isSyncing,
                    onClick = { viewModel.triggerManualSync() }
                )
                SettingSwitch(
                    icon = Icons.Default.Sync,
                    title = "Automatic Sync",
                    subtitle = "Keep local and cloud records synchronized",
                    checked = autoSync,
                    onCheckedChange = viewModel::setAutoSync
                )
                SettingSwitch(
                    icon = Icons.Default.CloudOff,
                    title = "Offline Mode",
                    subtitle = "Disable cloud requests",
                    checked = offlineMode,
                    onCheckedChange = viewModel::setOfflineMode
                )
            }

            SettingSection(title = "Location") {
                SettingSwitch(
                    icon = Icons.Default.MyLocation,
                    title = "High Accuracy GPS",
                    subtitle = "Use precise location for field work",
                    checked = highAccuracyGps,
                    onCheckedChange = viewModel::setHighAccuracyGps
                )
            }

            SettingSection(title = "Data") {
                SettingItem(
                    icon = Icons.Default.UploadFile,
                    title = "Export Data",
                    subtitle = "Export available job and invoice documents",
                    onClick = { viewModel.exportData() }
                )
                SettingItem(
                    icon = Icons.Default.Download,
                    title = "Import Data",
                    subtitle = "Pull records from Supabase",
                    onClick = { viewModel.importData() }
                )
            }

            SettingSection(title = "App Info") {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "Wildlife FieldOps",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    showChevron = false
                )
                SettingItem(
                    icon = Icons.Default.Storage,
                    title = "Local Database",
                    subtitle = "Room (SQLite)",
                    showChevron = false
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
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
    val clickModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) PrimaryGreen else TextTertiary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) TextPrimary else TextTertiary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        if (showChevron && onClick != null) {
            if (title == "Syncing…") {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
