package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.FieldCapturePhase
import com.strobingn.wildlifefieldops.ui.viewmodel.FieldCaptureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldCaptureScreen(
    sessionId: String? = null,
    onBack: () -> Unit,
    onCommittedJob: (jobId: String) -> Unit,
    viewModel: FieldCaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isListening by remember { mutableStateOf(false) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) viewModel.clearError()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result.values.any { it }
        if (hasLocationPermission) viewModel.refreshGps()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.addPhotoFromUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) viewModel.addPhotoFromBitmap(bitmap)
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isListening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
            }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) viewModel.appendSpeechResult(matches[0])
                isListening = false
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) viewModel.updateTranscript(matches[0])
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    LaunchedEffect(sessionId) {
        viewModel.start(sessionId)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfo()
        }
    }
    LaunchedEffect(state.committedJobId) {
        state.committedJobId?.let { jobId ->
            viewModel.clearCommittedJob()
            onCommittedJob(jobId)
        }
    }

    fun startListening() {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer.startListening(intent)
        isListening = true
    }

    fun stopListening() {
        runCatching { speechRecognizer.stopListening() }
        isListening = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Field Capture", fontWeight = FontWeight.Bold)
                        Text(
                            when (state.phase) {
                                FieldCapturePhase.CAPTURE -> "Voice · photos · GPS"
                                FieldCapturePhase.ANALYZING -> "Analyzing…"
                                FieldCapturePhase.REVIEW -> "Review fused draft"
                                FieldCapturePhase.SAVING -> "Saving job…"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.discard(onDone = onBack) },
                        enabled = state.phase != FieldCapturePhase.SAVING &&
                            state.phase != FieldCapturePhase.ANALYZING
                    ) {
                        Text("Discard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            !state.ready -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.phase == FieldCapturePhase.ANALYZING || state.phase == FieldCapturePhase.SAVING -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.phase == FieldCapturePhase.SAVING) {
                                "Saving job…"
                            } else {
                                "Running vision + fusion…"
                            }
                        )
                    }
                }
            }
            state.phase == FieldCapturePhase.REVIEW -> {
                ReviewPhase(
                    modifier = Modifier.padding(padding),
                    viewModel = viewModel,
                    state = state,
                    onBackToCapture = { viewModel.backToCapture() },
                    onSave = { viewModel.saveJob() }
                )
            }
            else -> {
                CapturePhase(
                    modifier = Modifier.padding(padding),
                    viewModel = viewModel,
                    state = state,
                    isListening = isListening,
                    onToggleMic = {
                        if (isListening) stopListening() else startListening()
                    },
                    onAddGallery = { galleryLauncher.launch("image/*") },
                    onAddCamera = {
                        if (!hasCameraPermission) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            cameraLauncher.launch(null)
                        }
                    },
                    onRefreshGps = {
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            viewModel.refreshGps()
                        }
                    },
                    onAnalyze = { viewModel.analyzeAndContinue() }
                )
            }
        }
    }
}

@Composable
private fun CapturePhase(
    modifier: Modifier,
    viewModel: FieldCaptureViewModel,
    state: com.strobingn.wildlifefieldops.ui.viewmodel.FieldCaptureUiState,
    isListening: Boolean,
    onToggleMic: () -> Unit,
    onAddGallery: () -> Unit,
    onAddCamera: () -> Unit,
    onRefreshGps: () -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Record what you see, add photos, refresh GPS, then analyze.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = state.transcript,
            onValueChange = viewModel::updateTranscript,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            label = { Text("Voice / field notes") },
            placeholder = { Text("e.g. Raccoon in attic chewed wires near the vent…") },
            minLines = 4,
            shape = RoundedCornerShape(14.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onToggleMic,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isListening) "Stop" else "Dictate")
            }
            OutlinedButton(
                onClick = onRefreshGps,
                enabled = !state.isRefreshingGps,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isRefreshingGps) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text("GPS")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Location", fontWeight = FontWeight.SemiBold)
                if (state.gpsLat != null && state.gpsLon != null) {
                    Text(
                        "%.5f, %.5f".format(state.gpsLat, state.gpsLon),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    state.gpsAccuracy?.let {
                        Text("Accuracy ±${it.toInt()} m", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.addressGuess.isNotBlank()) {
                        Text(state.addressGuess, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(
                        "No fix yet — tap GPS (requires location permission).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text("Photos (${state.photos.size})", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onAddCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(Modifier.width(8.dp))
                Text("Camera")
            }
            OutlinedButton(onClick = onAddGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text("Gallery")
            }
        }
        state.photos.forEach { photo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(photo.displayName, Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removePhoto(photo.photoId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove photo")
                    }
                }
            }
        }

        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Analyze & continue", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewPhase(
    modifier: Modifier,
    viewModel: FieldCaptureViewModel,
    state: com.strobingn.wildlifefieldops.ui.viewmodel.FieldCaptureUiState,
    onBackToCapture: () -> Unit,
    onSave: () -> Unit
) {
    val draft = state.draft
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.warnings.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Needs attention", fontWeight = FontWeight.SemiBold)
                    state.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (draft.needsReviewFields.isNotEmpty()) {
            Text(
                "Review fields: ${draft.needsReviewFields.joinToString()}",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedTextField(
            value = draft.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = draft.customerName,
            onValueChange = viewModel::updateCustomerName,
            label = { Text("Customer name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = draft.address,
            onValueChange = viewModel::updateAddress,
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Species", fontWeight = FontWeight.SemiBold)
        ChipRow(
            options = viewModel.speciesOptions,
            selectedIds = draft.speciesLabelIds.map { it.labelId }.toSet(),
            onToggle = viewModel::toggleSpecies
        )

        Text("Damage", fontWeight = FontWeight.SemiBold)
        ChipRow(
            options = viewModel.damageOptions,
            selectedIds = draft.damageLabelIds.map { it.labelId }.toSet(),
            onToggle = viewModel::toggleDamage
        )

        Text("Severity: ${draft.severity}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = draft.severity.toFloat(),
            onValueChange = { viewModel.updateSeverity(it.toInt()) },
            valueRange = 0f..4f,
            steps = 3
        )

        OutlinedTextField(
            value = draft.serviceType,
            onValueChange = viewModel::updateServiceType,
            label = { Text("Service type") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = draft.priority,
            onValueChange = viewModel::updatePriority,
            label = { Text("Priority") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = if (draft.estimatedPriceLow > 0) draft.estimatedPriceLow.toString() else "",
                onValueChange = viewModel::updatePriceLow,
                label = { Text("Price low") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = if (draft.estimatedPriceHigh > 0) draft.estimatedPriceHigh.toString() else "",
                onValueChange = viewModel::updatePriceHigh,
                label = { Text("Price high") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = draft.findings,
            onValueChange = viewModel::updateFindings,
            label = { Text("Findings") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            minLines = 3
        )
        OutlinedTextField(
            value = draft.recommendations,
            onValueChange = viewModel::updateRecommendations,
            label = { Text("Recommendations") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.entryPoints,
            onValueChange = viewModel::updateEntryPoints,
            label = { Text("Entry points") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.notes,
            onValueChange = viewModel::updateNotes,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Photos linked: ${draft.photoIds.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBackToCapture, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save job")
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selectedIds: Set<String>,
    onToggle: (id: String, display: String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, display) ->
            FilterChip(
                selected = id in selectedIds,
                onClick = { onToggle(id, display) },
                label = { Text(display) }
            )
        }
    }
}
