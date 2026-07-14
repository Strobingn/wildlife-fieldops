package com.strobingn.wildlifefieldops.ui.screens

// Heavy AI focused JobFormScreen with Grok hybrid + ML Kit + AR
// imePadding fixed, keyboard never blocks fields

 import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ai.AiAnalysisResult
import com.strobingn.wildlifefieldops.ai.HybridAIService
import com.strobingn.wildlifefieldops.ai.PhotoAIHelper
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
    var species by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var notes by remember { mutableStateOf("") }
    var isLoadingAI by remember { mutableStateOf(false) }
    var aiResultText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (jobId == null) "New Job + Heavy AI" else "Edit Job") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()  // KEY FIX: keyboard never blocks fields
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ========== HEAVY AI SECTION ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("HEAVY AI - Grok + ML Kit (Hudson Valley Wildlife Ops)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isLoadingAI = true
                                // Demo with placeholder URI - replace with real photo picker in next iteration
                                val demoUri = Uri.parse("content://demo")
                                val demoResult = HybridAIService.analyzePhotoAndFillForm(context, demoUri, "Hudson River / Newburgh area")
                                species = demoResult.species.joinToString()
                                serviceType = demoResult.suggestedServiceType
                                priority = demoResult.suggestedPriority
                                notes = demoResult.suggestedNotes
                                aiResultText = "AI filled: ${demoResult.species} | ${demoResult.suggestedServiceType} | Priority: ${demoResult.suggestedPriority}"
                                isLoadingAI = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoadingAI) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("ANALYZE PHOTO & AUTO-FILL FORM (Grok + ML Kit)")
                    }

                    Button(
                        onClick = { /* TODO: Wire real ARMeasurementHelper.launchARSession() */ },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("AR MEASURE DAMAGE / ENTRY POINT") }

                    Button(
                        onClick = {
                            scope.launch {
                                val estimate = HybridAIService.generateTieredEstimate(context, AiAnalysisResult(), "current job context")
                                aiResultText = estimate
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("AI GENERATE TIERED ESTIMATE (Good/Better/Best)") }

                    Button(
                        onClick = {
                            scope.launch {
                                val flags = HybridAIService.analyzeFormForCompliance(notes)
                                aiResultText = flags.joinToString("\n")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("AI COMPLIANCE AUDIT") }

                    if (aiResultText.isNotBlank()) {
                        Text(aiResultText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            // Standard fields (keyboard safe with imePadding)
            OutlinedTextField(value = species, onValueChange = { species = it }, label = { Text("Species (AI filled)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = serviceType, onValueChange = { serviceType = it }, label = { Text("Service Type") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("Priority") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (AI + Voice)") }, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 5)

            Button(onClick = { /* save logic using viewModel */ onNavigateBack() }, modifier = Modifier.fillMaxWidth()) {
                Text("SAVE JOB")
            }
        }
    }
}
