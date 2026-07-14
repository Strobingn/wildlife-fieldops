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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.wildlifefieldops.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Info Section
            SettingSection(title = "App Info") {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "Wildlife FieldOps",
                    subtitle = "Version 1.3.2"
                )
                SettingItem(
                    icon = Icons.Default.Build,
                    title = "Default Tax Rate",
                    subtitle = "8.125%"
                )
            }

            // AI Settings
            SettingSection(title = "AI Assistant") {
                SettingItem(
                    icon = Icons.Default.Psychology,
                    title = "Live AI",
                    subtitle = "Grok / OpenAI connected"
                )
                SettingItem(
                    icon = Icons.Default.Memory,
                    title = "On-device ML",
                    subtitle = "Photo analysis + form fill"
                )
            }

            // Data
            SettingSection(title = "Data") {
                SettingItem(
                    icon = Icons.Default.CloudSync,
                    title = "Sync",
                    subtitle = "Supabase cloud sync"
                )
                SettingItem(
                    icon = Icons.Default.Storage,
                    title = "Local Database",
                    subtitle = "Room (SQLite)"
                )
            }

            // Support
            SettingSection(title = "Support") {
                SettingItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Help & Documentation",
                    subtitle = "Field guides and tips"
                )
                SettingItem(
                    icon = Icons.Default.Policy,
                    title = "Compliance Notes",
                    subtitle = "NY/NJ wildlife regulations"
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
            title,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
