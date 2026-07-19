@file:androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
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
import com.strobingn.wildlifefieldops.ui.theme.*
import kotlinx.coroutines.tasks.await
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLKitCameraScreen(
    onPhotoCaptured: (String, List<String>, List<String>) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
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

    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
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
                Box(modifier = Modifier.weight(1f)) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        lifecycleOwner = lifecycleOwner,
                        imageCapture = imageCapture,
                        executor = executor,
                        onImageProxy = { imageProxy ->
                            if (isAnalyzing) return@CameraPreview
                            isAnalyzing = true

                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                CoroutineScope(Dispatchers.Default).launch {
                                    try {
                                        val labelResults = imageLabeler.process(image).await()
                                        labels = labelResults.map { "${it.text} (${(it.confidence * 100).toInt()}%)" }

                                        val objectResults = objectDetector.process(image).await()
                                        objects = objectResults.map { obj ->
                                            val category = obj.labels.firstOrNull()?.text ?: "Unknown"
                                            "$category (${(obj.labels.firstOrNull()?.confidence ?: 0f) * 100}%)"
                                        }

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

                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = PrimaryGreen
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            val photoFile = File(context.cacheDir, "mlkit_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        onPhotoCaptured(photoFile.absolutePath, labels, objects)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("MLKitCamera", "Photo capture failed", exception)
                                        Toast.makeText(context, "Photo capture failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = "Capture", modifier = Modifier.size(28.dp))
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
    executor: Executor,
    onImageProxy: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    ) { view ->
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(view.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor) { imageProxy ->
                        onImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
