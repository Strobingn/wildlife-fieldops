package com.strobingn.wildlifefieldops.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ai.ARMeasurementHelper
import com.strobingn.wildlifefieldops.ai.OnDeviceAIService
import com.strobingn.wildlifefieldops.ai.PhotoAIHelper
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.ui.viewmodel.JobsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFormScreen(
    jobId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var notes by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<com.strobingn.wildlifefieldops.ai.AiAnalysisResult?>(null) }
    var showEstimateDialog by remember { mutableStateOf(false) }
    var estimateSuggestions by remember { mutableStateOf<List<com.strobingn.wildlifefieldops.ai.EstimateSuggestion>>(emptyList()) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isAnalyzing = true
                val result = PhotoAIHelper.analyzePhotoForFormFilling(context, it)
                aiResult = result

                // HEAVY AI FORM FILLING - auto populate fields
                if (result.species.isNotEmpty()) species = result.species.joinToString(", ")
                if (result.suggestedServiceType.isNotBlank()) serviceType = result.suggestedServiceType
                if (result.suggestedPriority.isNotBlank()) priority = result.suggestedPriority
                if (result.suggestedNotes.isNotBlank()) notes = result.suggestedNotes

                isAnalyzing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (jobId == null) "New Job + AI Co-Pilot" else "Edit Job + AI") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("HEAVY AI POWERED JOB FORM", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

            // AI Analysis Card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("On-Device AI Photo Analysis & Auto-Fill", style = MaterialTheme.typography.titleMedium)
                    Text("Take or select photo → AI detects species/damage → instantly fills form fields + suggests estimate", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { photoLauncher.launch("image/*") }, enabled = !isAnalyzing) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isAnalyzing) "Analyzing..." else "AI Analyze Photo & Fill Form")
                        }
                        
                        Button(onClick = {
                            scope.launch {
                                val arSession = ARMeasurementHelper.createARSession(context)
                                if (arSession != null) {
                                    // In full impl: launch AR measurement Activity/Composable
                                    val measurement = ARMeasurementHelper.simulateMeasurementForDemo()
                                    notes = (notes + "\nAR Measured damage: ${measurement.distanceMeters}m (conf ${measurement.confidence})")
                                } else {
                                    notes = (notes + "\nAR not available on this device. Use manual measurement.")
                                }
                            }
                        }) {
                            Icon(Icons.Default.Straighten, null)
                            Spacer(Modifier.width(8.dp))
                            Text("AR Measure Damage")
                        }
                    }

                    aiResult?.let { result ->
                        Spacer(Modifier.height(8.dp))
                        Text("AI Results (confidence ${(result.confidence * 100).toInt()}%):", style = MaterialTheme.typography.labelLarge)
                        Text("Species: ${result.species.joinToString()}")
                        Text("Damage: ${result.damageTypes.joinToString()}")
                        Text("Suggested: ${result.suggestedServiceType} | Priority: ${result.suggestedPriority}")
                        Text("Est. Price: ${result.estimatedPriceRange}")
                    }
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = species, onValueChange = { species = it }, label = { Text("Species (AI filled)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = serviceType, onValueChange = { serviceType = it }, label = { Text("Service Type (AI filled)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("Priority") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (AI + AR filled)") }, modifier = Modifier.fillMaxWidth(), minLines = 4)

            // AI Estimate Generator
            Button(onClick = {
                scope.launch {
                    val analysis = aiResult ?: com.strobingn.wildlifefieldops.ai.AiAnalysisResult()
                    estimateSuggestions = OnDeviceAIService.generateEstimateFromAnalysis(analysis)
                    showEstimateDialog = true
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.RequestQuote, null)
                Spacer(Modifier.width(8.dp))
                Text("AI Generate Tiered Estimate (Good/Better/Best)")
            }

            Button(onClick = {
                scope.launch {
                    val job = Job(
                        id = jobId ?: java.util.UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        species = species,
                        serviceType = serviceType,
                        priority = priority,
                        notes = notes,
                        latitude = latitude,
                        longitude = longitude,
                        status = "PENDING"
                    )
                    if (jobId == null) viewModel.createJob(job) else viewModel.updateJobDetails(job)
                    onNavigateBack()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (jobId == null) "Create Job with AI Data" else "Save AI-Enhanced Job")
            }

            // Compliance quick check
            Button(onClick = {
                scope.launch {
                    val issues = OnDeviceAIService.analyzeFormForCompliance(notes + description, serviceType)
                    // Show in snackbar or dialog (simplified here)
                    notes = notes + "\n\nCompliance Check: ${issues.joinToString("; ")}"
                }
            }) {
                Text("AI Compliance Check on Current Form")
            }
        }
    }

    if (showEstimateDialog) {
        AlertDialog(
            onDismissRequest = { showEstimateDialog = false },
            title = { Text("AI Generated Estimates") },
            text = {
                Column {
                    estimateSuggestions.forEach { est ->
                        Text("${est.tier}: $${est.totalLow} - $${est.totalHigh}", style = MaterialTheme.typography.titleMedium)
                        est.lineItems.forEach { Text("• $it") }
                        Text(est.notes, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEstimateDialog = false }) { Text("Use Best Tier") } }
        )
    }
}