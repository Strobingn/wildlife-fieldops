package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.Session
import com.strobingn.wildlifefieldops.ai.ARMeasurementHelper
import com.strobingn.wildlifefieldops.ui.theme.BackgroundCard
import com.strobingn.wildlifefieldops.ui.theme.BackgroundDark
import com.strobingn.wildlifefieldops.ui.theme.PrimaryGreen
import com.strobingn.wildlifefieldops.ui.theme.StatusPending
import com.strobingn.wildlifefieldops.ui.theme.TextPrimary
import com.strobingn.wildlifefieldops.ui.theme.TextSecondary
import kotlin.math.sqrt

private data class MeasurePoint(
    val fracX: Float,
    val fracY: Float,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARMeasureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var session by remember { mutableStateOf<Session?>(null) }
    var renderer by remember { mutableStateOf<ARCameraRenderer?>(null) }
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var sessionError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var points by remember { mutableStateOf<List<MeasurePoint>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) sessionError = "Camera permission was denied."
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val arSupported = remember { ARMeasurementHelper.isARCoreSupported(context) }

    LaunchedEffect(hasCameraPermission, arSupported) {
        if (!hasCameraPermission || !arSupported || session != null) return@LaunchedEffect

        val created = ARMeasurementHelper.createARSession(context)
        if (created == null) {
            sessionError = "ARCore could not start. Update Google Play Services for AR, then reopen AR Measurement."
        } else {
            session = created
        }
    }

    DisposableEffect(session, lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        session?.resume()
                        glSurfaceView?.onResume()
                    } catch (error: Exception) {
                        sessionError = "AR camera could not resume: ${error.message ?: error.javaClass.simpleName}"
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    glSurfaceView?.onPause()
                    runCatching { session?.pause() }
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                session?.resume()
                glSurfaceView?.onResume()
            } catch (error: Exception) {
                sessionError = "AR camera could not start: ${error.message ?: error.javaClass.simpleName}"
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView?.onPause()
            runCatching { session?.pause() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { session?.close() }
        }
    }

    val totalMeters = remember(points) {
        points.zipWithNext().sumOf { (a, b) ->
            sqrt(
                ((b.worldX - a.worldX) * (b.worldX - a.worldX) +
                    (b.worldY - a.worldY) * (b.worldY - a.worldY) +
                    (b.worldZ - a.worldZ) * (b.worldZ - a.worldZ)).toDouble()
            )
        }
    }
    val totalFeet = totalMeters * 3.28084

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AR Measurement", color = TextPrimary) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                !hasCameraPermission -> {
                    InfoCard("Camera permission is required for AR measurement.") {
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant permission")
                        }
                    }
                }

                !arSupported -> {
                    InfoCard(
                        "This phone does not report ARCore support. Install or update Google Play Services for AR and try again."
                    ) {}
                }

                sessionError != null -> {
                    InfoCard(sessionError!!) {
                        Button(onClick = {
                            sessionError = null
                            cameraReady = false
                            runCatching { session?.close() }
                            session = null
                            renderer = null
                            glSurfaceView = null
                        }) {
                            Text("Retry")
                        }
                    }
                }

                session == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryGreen)
                            Spacer(Modifier.height(12.dp))
                            Text("Starting ARCore…", color = TextSecondary)
                        }
                    }
                }

                else -> {
                    Text(
                        "Move the phone slowly until surfaces are detected, then tap measurement points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val currentSession = session!!
                        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { viewContext ->
                                val activity = viewContext as? Activity
                                val arRenderer = ARCameraRenderer(
                                    session = currentSession,
                                    displayRotation = {
                                        @Suppress("DEPRECATION")
                                        activity?.windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                                    },
                                    onFrameReady = {
                                        cameraReady = true
                                        statusMessage = "Camera ready — move slowly to detect surfaces"
                                    },
                                    onHit = { hit ->
                                        points = points + MeasurePoint(
                                            fracX = hit.fractionX,
                                            fracY = hit.fractionY,
                                            worldX = hit.worldX,
                                            worldY = hit.worldY,
                                            worldZ = hit.worldZ
                                        )
                                        statusMessage = null
                                    },
                                    onNoSurface = {
                                        statusMessage = "No tracked surface there yet — move slowly and try again"
                                    },
                                    onError = { message ->
                                        sessionError = "AR camera error: $message"
                                    }
                                )
                                renderer = arRenderer

                                GLSurfaceView(viewContext).apply {
                                    setEGLContextClientVersion(2)
                                    preserveEGLContextOnPause = true
                                    setRenderer(arRenderer)
                                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                                    glSurfaceView = this
                                }
                            },
                            update = { view ->
                                glSurfaceView = view
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(renderer, cameraReady) {
                                    detectTapGestures { offset ->
                                        if (cameraReady) {
                                            renderer?.queueTap(offset.x, offset.y)
                                            statusMessage = "Finding surface…"
                                        }
                                    }
                                }
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val renderedPoints = points.map {
                                Offset(it.fracX * size.width, it.fracY * size.height)
                            }
                            renderedPoints.zipWithNext().forEach { (start, end) ->
                                drawLine(Color(0xFF3B7AE8), start, end, strokeWidth = 5f)
                            }
                            renderedPoints.forEach { point ->
                                drawCircle(Color(0xFF3B7AE8), radius = 14f, center = point)
                                drawCircle(Color.White, radius = 6f, center = point)
                            }
                        }

                        if (!cameraReady) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = PrimaryGreen)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Opening AR camera…", color = Color.White)
                                }
                            }
                        }
                    }

                    statusMessage?.let {
                        Text(it, color = StatusPending, style = MaterialTheme.typography.labelSmall)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Total: ${String.format("%.1f", totalFeet)} ft",
                                style = MaterialTheme.typography.titleLarge,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${points.size} point${if (points.size == 1) "" else "s"} placed · " +
                                    "${points.zipWithNext().size} segment(s)" +
                                    if (points.size >= 2) {
                                        " · last: ${String.format("%.1f", segmentFeet(points.last(), points[points.size - 2]))} ft"
                                    } else {
                                        ""
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { points = points.dropLast(1) },
                            enabled = points.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, tint = TextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("Undo", color = TextPrimary)
                        }

                        OutlinedButton(
                            onClick = { points = emptyList() },
                            enabled = points.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(
                                        "Linear footage: ${String.format("%.1f", totalFeet)} ft " +
                                            "(${points.size} points, AR measured)"
                                    )
                                )
                            },
                            enabled = points.size >= 2,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Copy ft", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(text: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text, color = TextPrimary)
            content()
        }
    }
}

private fun segmentFeet(a: MeasurePoint, b: MeasurePoint): Double {
    return sqrt(
        ((a.worldX - b.worldX) * (a.worldX - b.worldX) +
            (a.worldY - b.worldY) * (a.worldY - b.worldY) +
            (a.worldZ - b.worldZ) * (a.worldZ - b.worldZ)).toDouble()
    ) * 3.28084
}
