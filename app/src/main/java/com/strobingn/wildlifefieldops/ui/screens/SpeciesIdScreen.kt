package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.SpeciesIdViewModel
import java.io.File

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

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraPermissionRequested by remember { mutableStateOf(false) }
    var pendingCameraLaunch by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }
    // Full-resolution capture into our own file via FileProvider. The old
    // TakePicturePreview thumbnail comes back null on several devices/camera apps,
    // which made the Camera button silently do nothing.
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = tempPhotoUri
        if (success && uri != null) {
            viewModel.onImageSelected(uri)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (granted && pendingCameraLaunch) {
            pendingCameraLaunch = false
            val (uri, error) = launchCameraForResult(context, takePictureLauncher)
            tempPhotoUri = uri
            cameraError = error
        }
    }

    fun startCamera() {
        cameraError = null
        if (!hasCameraPermission) {
            pendingCameraLaunch = true
            cameraPermissionRequested = true
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val (uri, error) = launchCameraForResult(context, takePictureLauncher)
        tempPhotoUri = uri
        cameraError = error
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
                    onClick = { startCamera() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(6.dp))
                    Text("Camera", color = TextPrimary)
                }
            }

            // Camera permission rationale / recovery — never leave a dead button.
            if (!hasCameraPermission && cameraPermissionRequested) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Camera permission is needed to photograph the animal. (Gallery works without it.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { startCamera() }) {
                                Text("Grant camera access", color = PrimaryGreen)
                            }
                            val activity = context.findActivity()
                            if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                                TextButton(onClick = { openAppSettings(context) }) {
                                    Text("Open Settings", color = PrimaryGreen)
                                }
                            }
                        }
                    }
                }
            }
            cameraError?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = ErrorRed)
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

/** Creates a capture target and fires the system camera. Returns (uri, errorMessage). */
private fun launchCameraForResult(
    context: Context,
    launcher: ActivityResultLauncher<Uri>
): Pair<Uri?, String?> {
    return try {
        val uri = createSpeciesImageUri(context)
        launcher.launch(uri)
        uri to null
    } catch (e: ActivityNotFoundException) {
        null to "No camera app found on this device. Use Gallery instead."
    } catch (e: Exception) {
        null to "Couldn't open the camera: ${e.message ?: e.javaClass.simpleName}"
    }
}

private fun createSpeciesImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "species").apply { mkdirs() }
    val file = File(dir, "species_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
