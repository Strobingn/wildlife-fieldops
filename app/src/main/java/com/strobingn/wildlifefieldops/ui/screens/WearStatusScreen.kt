package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.wear.WearCompanionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearStatusScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wear OS", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Watch, null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Wear Companion",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (WearCompanionHelper.isWearAvailable()) "Connected" else "Ready for pairing",
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Phone-side Data Layer paths prepared. Full Wear module can be added as :wear without breaking this APK.",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Supported paths", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(WearCompanionHelper.PATH_JOB_STATUS, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(WearCompanionHelper.PATH_QUICK_NOTE, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(WearCompanionHelper.PATH_TRAP_CHECK, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
