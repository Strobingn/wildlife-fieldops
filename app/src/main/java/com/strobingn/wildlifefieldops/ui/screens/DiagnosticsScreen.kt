package com.strobingn.wildlifefieldops.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.BuildConfig
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.MlDiagnosticsViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    mlViewModel: MlDiagnosticsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val autoSync by viewModel.autoSync.collectAsState(initial = true)
    val offlineMode by viewModel.offlineMode.collectAsState(initial = false)
    val highAccuracyGps by viewModel.highAccuracyGps.collectAsState(initial = true)
    val ml by mlViewModel.ui.collectAsState()
    var networkStatus by remember { mutableStateOf(readNetworkStatus(context)) }

    LaunchedEffect(Unit) {
        mlViewModel.refresh()
    }

    LaunchedEffect(ml.pendingShareIntent) {
        ml.pendingShareIntent?.let { intent ->
            runCatching { context.startActivity(intent) }
            mlViewModel.consumeShareIntent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Diagnostics", color = TextPrimary) },
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
            DiagnosticSection("Build") {
                DiagnosticRow("App version", BuildConfig.VERSION_NAME, true)
                DiagnosticRow("Build number", BuildConfig.VERSION_CODE.toString(), true)
                DiagnosticRow("Android", Build.VERSION.RELEASE, true)
                DiagnosticRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}", true)
            }

            DiagnosticSection("Configuration") {
                val supabaseReady = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
                val mapsReady = BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank()
                val aiReady = BuildConfig.LLM_API_KEY.isNotBlank()
                DiagnosticRow("Supabase", if (supabaseReady) "Configured" else "Missing", supabaseReady)
                DiagnosticRow("Google Maps", if (mapsReady) "Configured" else "Missing", mapsReady)
                DiagnosticRow("AI", if (aiReady) "Configured" else "Missing", aiReady)
                DiagnosticRow(
                    "Service status",
                    connectionStatus,
                    !connectionStatus.contains("missing", ignoreCase = true)
                )
            }

            DiagnosticSection("ML / Field Capture") {
                DiagnosticRow("Training labels", ml.trainingLabelCount.toString(), ml.trainingLabelCount >= 0)
                DiagnosticRow(
                    "Unexported labels",
                    ml.unexportedLabelCount.toString(),
                    ml.unexportedLabelCount == 0 || ml.trainingLabelCount > 0
                )
                DiagnosticRow("Vision predictions", ml.visionPredictionCount.toString(), true)
                DiagnosticRow("Capture sessions", ml.captureSessionCount.toString(), true)
                DiagnosticRow("Committed captures", ml.committedSessionCount.toString(), true)
                DiagnosticRow(
                    "TFLite model flag",
                    if (ml.mlTfliteEnabled) "Enabled" else "Disabled (ML Kit path)",
                    !ml.mlTfliteEnabled || ml.mlTfliteEnabled
                )
                DiagnosticRow(
                    "Cloud VLM flag",
                    if (ml.mlCloudVlmEnabled) "Enabled" else "Disabled",
                    true
                )
                DiagnosticRow("Taxonomy asset", ml.taxonomyAsset, true)
                DiagnosticRow(
                    "Last capture error",
                    ml.lastSessionError.ifBlank { "None" },
                    ml.lastSessionError.isBlank()
                )
            }

            Button(
                onClick = { mlViewModel.exportLabels(unexportedOnly = false) },
                enabled = !ml.isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ml.isExporting) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Exporting…")
                } else {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export training labels (JSONL)")
                }
            }

            OutlinedButton(
                onClick = { mlViewModel.exportLabels(unexportedOnly = true) },
                enabled = !ml.isExporting && ml.unexportedLabelCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export unexported only (${ml.unexportedLabelCount})")
            }

            if (!ml.message.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = ml.message ?: "",
                        color = TextPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            DiagnosticSection("Runtime") {
                DiagnosticRow("Network", networkStatus, networkStatus != "Offline")
                DiagnosticRow("Auto sync", if (autoSync) "Enabled" else "Disabled", autoSync)
                DiagnosticRow("Offline mode", if (offlineMode) "Enabled" else "Disabled", !offlineMode)
                DiagnosticRow(
                    "High accuracy GPS",
                    if (highAccuracyGps) "Enabled" else "Disabled",
                    highAccuracyGps
                )
            }

            Button(
                onClick = {
                    networkStatus = readNetworkStatus(context)
                    mlViewModel.refresh()
                    viewModel.triggerManualSync()
                },
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSyncing) "Running diagnostics…" else "Test Connection + Full Sync")
            }

            if (!syncMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = syncMessage ?: "",
                        color = TextPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (ok) PrimaryGreen else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(value, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun readNetworkStatus(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return "Offline"
    val caps = cm.getNetworkCapabilities(network) ?: return "Offline"
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi online"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular online"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet online"
        else -> "Online"
    }
}
