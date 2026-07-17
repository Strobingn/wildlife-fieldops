package com.strobingn.wildlifefieldops.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.SpeciesIdViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesIdScreen(
    onBack: () -> Unit,
    viewModel: SpeciesIdViewModel = hiltViewModel()
) {
    val imageUri by viewModel.imageUri.collectAsState()
    val analyzing by viewModel.analyzing.collectAsState()
    val result by viewModel.result.collectAsState()
    val message by viewModel.message.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { viewModel.onCameraBitmap(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Species ID", color = TextPrimary) },
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
            // Photo preview
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Photo to identify",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Photograph the animal, tracks, scat, or damage",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Capture buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery", color = TextPrimary)
                }
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(6.dp))
                    Text("Camera", color = TextPrimary)
                }
            }

            Button(
                onClick = { viewModel.analyze() },
                enabled = imageUri != null && !analyzing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (analyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Identifying…")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Identify Species", fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                if (viewModel.isConfigured) "On-device ML + ${viewModel.providerLabel} vision" else "On-device ML only (no AI key configured)",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = ErrorRed) }

            // Result
            result?.let { r ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.species, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                                if (r.scientificName.isNotBlank()) {
                                    Text(r.scientificName, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                }
                            }
                            if (r.confidence.isNotBlank()) {
                                Surface(
                                    color = PrimaryGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        r.confidence.uppercase(),
                                        color = PrimaryGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        if (!r.fromAi) {
                            Text("Offline result — verify visually on site", style = MaterialTheme.typography.labelSmall, color = StatusPending)
                        }

                        Divider(color = DividerDark)

                        SpeciesIdRow("NY legal status", r.protectedStatusNY)
                        SpeciesIdRow("Rabies vector risk", r.rabiesVectorRisk)
                        SpeciesIdRow("Behavior", r.behaviorNotes)
                        if (r.recommendedApproach.isNotEmpty()) {
                            Column {
                                Text("Recommended approach", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                r.recommendedApproach.forEach {
                                    Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }
                        }
                        SpeciesIdRow("Trap & bait", r.trapAndBait)
                        if (r.safetyWarnings.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusPending, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(r.safetyWarnings, style = MaterialTheme.typography.bodySmall, color = StatusPending)
                            }
                        }
                    }
                }

                TextButton(onClick = { viewModel.clear() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Identify another photo", color = PrimaryGreen)
                }
            }
        }
    }
}

@Composable
private fun SpeciesIdRow(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
