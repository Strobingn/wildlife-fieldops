package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.strobingn.wildlifefieldops.BuildConfig
import com.strobingn.wildlifefieldops.ui.theme.BackgroundCard
import com.strobingn.wildlifefieldops.ui.theme.BackgroundDark
import com.strobingn.wildlifefieldops.ui.theme.PrimaryGreen
import com.strobingn.wildlifefieldops.ui.theme.TextPrimary
import com.strobingn.wildlifefieldops.ui.theme.TextSecondary

private data class ProviderStatus(
    val name: String,
    val description: String,
    val configured: Boolean,
    val actionLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationSettingsScreen(onBack: () -> Unit) {
    val providers = listOf(
        ProviderStatus("Supabase", "Authentication, database, storage and sync", BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank(), "Configured at build time"),
        ProviderStatus("xAI / Grok", "AI assistant and field operations intelligence", BuildConfig.LLM_API_KEY.isNotBlank() && BuildConfig.LLM_BASE_URL.contains("x.ai"), "Connect with API key"),
        ProviderStatus("OpenAI", "Optional ChatGPT/OpenAI provider", BuildConfig.LLM_API_KEY.isNotBlank() && BuildConfig.LLM_BASE_URL.contains("openai.com"), "Connect account or API key"),
        ProviderStatus("Google Maps", "Mapping, routing and geocoding", BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank(), "Configure API key"),
        ProviderStatus("OpenWeather", "Weather-aware scheduling", BuildConfig.OPENWEATHER_API_KEY.isNotBlank(), "Configure API key"),
        ProviderStatus("Stripe / Square", "Payments and deposits", false, "Connect provider"),
        ProviderStatus("Twilio / Email", "SMS and customer notifications", false, "Connect provider"),
        ProviderStatus("Firebase", "Push notifications", false, "Connect project")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Integrations", color = TextPrimary) },
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
            Text(
                "Provider credentials are never displayed. Connected services are detected from the secure build and backend configuration.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            providers.forEach { provider ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (provider.configured) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (provider.configured) PrimaryGreen else Color.Gray
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(provider.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(provider.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (provider.configured) "Connected" else "Not connected",
                                color = if (provider.configured) PrimaryGreen else TextSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        TextButton(onClick = { }) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(provider.actionLabel)
                        }
                    }
                }
            }
        }
    }
}
