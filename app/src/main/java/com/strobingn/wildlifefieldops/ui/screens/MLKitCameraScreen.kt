package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import android.os.Environment
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.Photo
import com.strobingn.wildlifefieldops.data.model.PhotoCategory
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.PhotosViewModel
import kotlinx.coroutines.tasks.await
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLKitCameraScreen(
    onPhotoCaptured: (String, List<String>, List<String>) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    photosViewModel: PhotosViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isAnalyzing by remember { mutableStateOf(false) }
    var labels by remember { mutableStateOf(listOf<String>()) }
    var objects by remember { mutableStateOf(listOf<String>()) }
    var speciesGuess by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(0f) }
    var captureMessage by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraBindRetry by remember { mutableStateOf(0) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val scope = rememberCoroutineScope()

    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageLabeler = remember { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }
    val objectDetector = remember {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            imageLabeler.close()
            objectDetector.close()
            executor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Photo Analysis", color = TextPrimary) },
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
        ) {
            if (!hasCameraPermission) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NoPhotography,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Camera permission required", color = TextSecondary)
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                // Camera preview
                Box(modifier = Modifier.weight(1f)) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        lifecycleOwner = lifecycleOwner,
                        imageCapture = imageCapture,
                        analysisExecutor = executor,
                        bindRetry = cameraBindRetry,
                        onCameraError = { cameraError = it },
                        onImageProxy = { imageProxy ->
                            if (isAnalyzing) return@CameraPreview
                            isAnalyzing = true

                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                // Run both detectors
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        // Image labeling
                                        val labelResults = imageLabeler.process(image).await()
                                        labels = labelResults.map { "${it.text} (${(it.confidence * 100).toInt()}%)" }

                                        // Object detection
                                        val objectResults = objectDetector.process(image).await()
                                        objects = objectResults.map { obj ->
                                            val category = obj.labels.firstOrNull()?.text ?: "Unknown"
                                            "$category (${(obj.labels.firstOrNull()?.confidence ?: 0f) * 100}%)"
                                        }

                                        // Species guess from labels
                                        val wildlifeLabels = listOf(
                                            "raccoon", "squirrel", "bat", "skunk", "snake",
                                            "bird", "rodent", "fox", "deer", "bear", "coyote",
                                            "opossum", "chipmunk", "mouse", "rat"
                                        )
                                        val matched = labelResults.firstOrNull { label ->
                                            wildlifeLabels.any { label.text.lowercase().contains(it) }
                                        }
                                        speciesGuess = matched?.text ?: "No wildlife detected"
                                        confidence = matched?.confidence ?: 0f

                                    } catch (e: Exception) {
                                        Log.e("MLKitCamera", "Analysis error", e)
                                    } finally {
                                        imageProxy.close()
                                        isAnalyzing = false
                                    }
                                }
                            } else {
                                imageProxy.close()
                                isAnalyzing = false
                            }
                        }
                    )

                    // Camera failure — visible error + retry instead of a black box.
                    cameraError?.let { error ->
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.NoPhotography,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextSecondary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(error, color = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { cameraBindRetry++ },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Text("Retry camera")
                            }
                        }
                    }

                    // Overlay labels
                    if (labels.isNotEmpty() || speciesGuess.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (speciesGuess.isNotBlank()) {
                                        Text(
                                            "Species: $speciesGuess",
                                            color = PrimaryGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (confidence > 0) {
                                            Text(
                                                "Confidence: ${(confidence * 100).toInt()}%",
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    if (labels.isNotEmpty()) {
                                        Text("Detected:", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        labels.take(5).forEach { label ->
                                            Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Analyzing indicator
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = PrimaryGreen
                        )
                    }
                }

                // Capture status feedback
                captureMessage?.let {
                    Text(
                        it,
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Bottom controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            if (isCapturing) return@FilledIconButton
                            isCapturing = true
                            captureMessage = null
                            val photoDir = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "ai_captures"
                            ).apply { mkdirs() }
                            val photoFile = File(
                                photoDir,
                                "AI_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            imageCapture.takePicture(
                                outputOptions,
                                executor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val labelTexts = labels
                                        val objectTexts = objects
                                        scope.launch(Dispatchers.Main) {
                                            val description = buildString {
                                                append("AI capture")
                                                if (speciesGuess.isNotBlank() && speciesGuess != "No wildlife detected") {
                                                    append(" — species: $speciesGuess")
                                                }
                                                if (labelTexts.isNotEmpty()) {
                                                    append(" · ${labelTexts.take(3).joinToString(", ")}")
                                                }
                                            }
                                            photosViewModel.savePhoto(
                                                Photo(
                                                    filePath = photoFile.absolutePath,
                                                    localPath = photoFile.absolutePath,
                                                    category = PhotoCategory.JOB_SITE,
                                                    description = description,
                                                    takenAt = System.currentTimeMillis(),
                                                    fileSize = photoFile.length()
                                                )
                                            )
                                            captureMessage = "Saved to Photo Gallery: ${photoFile.name}"
                                            isCapturing = false
                                            onPhotoCaptured(photoFile.absolutePath, labelTexts, objectTexts)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("MLKitCamera", "Capture failed", exception)
                                        scope.launch(Dispatchers.Main) {
                                            captureMessage = "Capture failed: ${exception.message ?: "unknown error"}"
                                            isCapturing = false
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier.size(64.dp),
                        enabled = !isCapturing,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryGreen)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Camera, contentDescription = "Capture", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture,
    analysisExecutor: Executor,
    bindRetry: Int,
    onCameraError: (String?) -> Unit,
    onImageProxy: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    // Bind ONCE per (view, lifecycle, retry) — not in AndroidView's update block. The old
    // code added a new provider listener on every recomposition (ML results recompose the
    // screen several times per second), so the camera was constantly unbound/rebound and
    // the preview flickered or stayed black.
    LaunchedEffect(previewView, lifecycleOwner, bindRetry) {
        onCameraError(null)

        val cameraProvider = runCatching { awaitCameraProvider(context) }.getOrElse {
            Log.e("CameraPreview", "Camera provider unavailable", it)
            onCameraError("Camera is not available on this device.")
            return@LaunchedEffect
        }

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(analysisExecutor) { imageProxy ->
                    onImageProxy(imageProxy)
                }
            }

        fun bind(selector: CameraSelector) {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture,
                imageAnalysis
            )
        }

        try {
            bind(CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Back-camera binding failed, trying front camera", e)
            try {
                bind(CameraSelector.DEFAULT_FRONT_CAMERA)
            } catch (e2: Exception) {
                Log.e("CameraPreview", "Front-camera binding failed", e2)
                onCameraError("Couldn't start the camera. Close other camera apps, then tap Retry.")
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private suspend fun awaitCameraProvider(context: android.content.Context): ProcessCameraProvider =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cont.resume(future.get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
