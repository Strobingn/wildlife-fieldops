package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.JobsViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.VoiceJobViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceJobScreen(
    onBack: () -> Unit,
    viewModel: VoiceJobViewModel = hiltViewModel(),
    jobsViewModel: JobsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var jobCreated by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val parsing by viewModel.parsing.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val vmMessage by viewModel.message.collectAsState()

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) errorMessage = "Microphone permission is required for voice jobs"
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { errorMessage = ""; isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try again."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Check connection."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Try speaking louder."
                    else -> "Speech recognition error: $error"
                }
            }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) transcript = matches[0]
                isListening = false
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) transcript = matches[0]
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    fun startListening() {
        if (!hasPermission) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = "Speech recognition not available on this device"; return
        }
        viewModel.clearDraft()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.US)
        }
        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Job", color = TextPrimary) },
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
                "Dictate the job hands-free: customer, address, species, problem, when. AI builds the job card.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Mic
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconButton(
                        onClick = { if (isListening) stopListening() else startListening() },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isListening) ErrorRed else PrimaryGreen
                        )
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop" else "Start",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isListening) "Listening… tap to stop" else "Tap to speak",
                        color = if (isListening) ErrorRed else TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (isListening) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryGreen, trackColor = BackgroundCard)
            }
            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
            }

            // Editable transcript
            OutlinedTextField(
                value = transcript,
                onValueChange = { transcript = it; viewModel.clearDraft() },
                label = { Text("Transcript (editable)") },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { viewModel.parse(transcript) },
                enabled = transcript.isNotBlank() && !parsing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (parsing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Parsing job…")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Parse Job with AI", fontWeight = FontWeight.SemiBold)
                }
            }
            vmMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = StatusPending) }

            // Parsed draft preview + create
            draft?.let { d ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Job draft", color = TextPrimary, fontWeight = FontWeight.Bold)
                        VoiceJobRow("Title", d.title)
                        VoiceJobRow("Customer", d.customerName.ifBlank { "—" })
                        VoiceJobRow("Address", d.address.ifBlank { "—" })
                        VoiceJobRow("Service", d.serviceType.ifBlank { "Inspection" })
                        VoiceJobRow("Priority", d.priority)
                        if (d.scheduledText.isNotBlank()) VoiceJobRow("Requested time", d.scheduledText)
                        if (d.notes.isNotBlank()) VoiceJobRow("Notes", d.notes)
                    }
                }

                Button(
                    onClick = {
                        val notes = buildString {
                            if (d.scheduledText.isNotBlank()) appendLine("Requested time: ${d.scheduledText}")
                            append(d.notes)
                            if (isNotBlank()) append("\n")
                            append("[Created by voice]")
                        }
                        jobsViewModel.createJob(
                            d.title.ifBlank { "Voice job" },
                            d.notes,
                            "",
                            d.customerName,
                            d.address,
                            d.serviceType.ifBlank { "Inspection" },
                            runCatching { JobPriority.valueOf(d.priority.uppercase()) }.getOrDefault(JobPriority.MEDIUM),
                            0.0,
                            null,
                            notes
                        )
                        jobCreated = true
                    },
                    enabled = !jobCreated,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Job", fontWeight = FontWeight.Bold)
                }
            }

            if (jobCreated) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Job created. New customer names are auto-added to Customers.",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Done", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun VoiceJobRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
