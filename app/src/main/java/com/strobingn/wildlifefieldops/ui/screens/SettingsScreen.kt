package com.strobingn.wildlifefieldops.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkTheme by viewModel.darkTheme.collectAsState(initial = true)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = true)
    val autoSync by viewModel.autoSync.collectAsState(initial = true)
    val syncInterval by viewModel.syncInterval.collectAsState(initial = 15)
    val companyName by viewModel.companyName.collectAsState(initial = "Wildlife Whisperer LLC")
    val technicianName by viewModel.technicianName.collectAsState(initial = "")
    val defaultTaxRate by viewModel.defaultTaxRate.collectAsState(initial = 0f)
    val offlineMode by viewModel.offlineMode.collectAsState(initial = false)
    val highAccuracyGps by viewModel.highAccuracyGps.collectAsState(initial = true)
    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Company Info
            SettingsSectionTitle("Company Information")
            SettingsCard {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = viewModel::setCompanyName,
                    label = { Text("Company Name") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = TextSecondary) },
                    colors = settingFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = technicianName,
                    onValueChange = viewModel::setTechnicianName,
                    label = { Text("Default Technician Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                    colors = settingFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = defaultTaxRate.toString(),
                    onValueChange = {
                        it.toFloatOrNull()?.let { rate -> viewModel.setDefaultTaxRate(rate) }
                    },
                    label = { Text("Default Tax Rate (%)") },
                    leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = TextSecondary) },
                    colors = settingFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Appearance
            SettingsSectionTitle("Appearance")
            SettingsCard {
                SettingsSwitchItem(
                    title = "Dark Theme",
                    subtitle = "Use dark color scheme",
                    icon = Icons.Default.DarkMode,
                    checked = darkTheme,
                    onCheckedChange = viewModel::setDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Notifications
            SettingsSectionTitle("Notifications")
            SettingsCard {
                SettingsSwitchItem(
                    title = "Enable Notifications",
                    subtitle = "Receive alerts and reminders",
                    icon = Icons.Default.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sync & Data
            SettingsSectionTitle("Sync & Data")
            SettingsCard {
                SettingsSwitchItem(
                    title = "Auto Sync",
                    subtitle = "Automatically sync with cloud",
                    icon = Icons.Default.Sync,
                    checked = autoSync,
                    onCheckedChange = viewModel::setAutoSync
                )
                SettingsSwitchItem(
                    title = "Offline Mode",
                    subtitle = "Work without internet connection",
                    icon = Icons.Default.CloudOff,
                    checked = offlineMode,
                    onCheckedChange = viewModel::setOfflineMode
                )
                SettingsSwitchItem(
                    title = "High Accuracy GPS",
                    subtitle = "Use GPS for precise location",
                    icon = Icons.Default.GpsFixed,
                    checked = highAccuracyGps,
                    onCheckedChange = viewModel::setHighAccuracyGps
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.triggerManualSync() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.exportData() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                    OutlinedButton(
                        onClick = { viewModel.importData() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danger Zone
            SettingsSectionTitle("Danger Zone")
            SettingsCard {
                Button(
                    onClick = { showClearDataDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Data", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Wildlife FieldOps v1.1",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?", color = TextPrimary) },
            text = { Text("This will permanently delete all jobs, customers, inspections, and photos. This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDataDialog = false
                }) {
                    Text("Delete Everything", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BackgroundCard
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryGreen,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen, checkedTrackColor = PrimaryGreen.copy(alpha = 0.5f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun settingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = BorderDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = BackgroundDark,
    unfocusedContainerColor = BackgroundDark
)
