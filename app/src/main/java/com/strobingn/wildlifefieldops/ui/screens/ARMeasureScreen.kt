package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.DepthPoint
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.exceptions.NotYetAvailableException
import com.strobingn.wildlifefieldops.ai.ARMeasurementHelper
import com.strobingn.wildlifefieldops.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
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
fun ARMeasureScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val arSupported = remember { ARMeasurementHelper.isARCoreSupported(context) }

    var session by remember { mutableStateOf<Session?>(null) }
    LaunchedEffect(hasCameraPermission, arSupported) {
        if (hasCameraPermission && arSupported && session == null) {
            session = ARMeasurementHelper.createARSession(context)
        }
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var points by remember { mutableStateOf<List<MeasurePoint>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var sessionError by remember { mutableStateOf<String?>(null) }

    // Pending taps in preview-fraction coordinates, consumed by the AR thread
    var pendingTaps by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val rotationDegrees = remember {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        when (wm.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> 90
        }
    }

    // Pause/resume with lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(session, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> runCatching { session?.pause() }
                Lifecycle.Event.ON_RESUME -> runCatching { session?.resume() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { session?.pause() }
            runCatching { session?.close() }
        }
    }

    // AR frame loop on a dedicated single thread (session is not thread-safe)
    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        val arDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            withContext(arDispatcher) {
                runCatching { s.resume() }
                while (isActive) {
                    try {
                        val frame = s.update()
                        // Camera frame -> preview bitmap
                        try {
                            frame.acquireCameraImage().use { img ->
                                val bmp = yuvToBitmap(img, rotationDegrees)
                                withContext(Dispatchers.Main) { previewBitmap = bmp }
                            }
                        } catch (e: NotYetAvailableException) { /* frame not ready */ }

                        // Consume one pending tap per loop
                        val tap = pendingTaps.firstOrNull()
                        if (tap != null) {
                            val bmpW = previewBitmap?.width ?: 0
                            val bmpH = previewBitmap?.height ?: 0
                            var placed = false
                            if (bmpW > 0 && bmpH > 0) {
                                val hits = frame.hitTest(tap.x * bmpW, tap.y * bmpH)
                                val hit = hits.firstOrNull { it.trackable is Plane || it.trackable is DepthPoint }
                                    ?: hits.firstOrNull()
                                if (hit != null) {
                                    val pose = hit.hitPose
                                    val newPoint = MeasurePoint(
                                        fracX = tap.x, fracY = tap.y,
                                        worldX = pose.tx(), worldY = pose.ty(), worldZ = pose.tz()
                                    )
                                    withContext(Dispatchers.Main) { points = points + newPoint }
                                    placed = true
                                }
                            }
                            withContext(Dispatchers.Main) {
                                pendingTaps = pendingTaps.drop(1)
                                if (!placed) statusMessage = "No surface found there — tap on a wall, roofline, or ground"
                            }
                        }
                    } catch (e: Exception) {
                        // Session likely paused (lifecycle); keep the loop alive
                        delay(250)
                    }
                    delay(90)
                }
            }
        } finally {
            arDispatcher.close()
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
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant permission") }
                    }
                }
                !arSupported -> {
                    InfoCard("This device does not support ARCore. AR measurement needs an ARCore-compatible phone (most Samsung/Pixel/modern Android). Measure manually and enter footage in the estimate.") {}
                }
                sessionError != null -> InfoCard(sessionError!!) {}
                else -> {
                    Text(
                        "Tap along the roofline/soffit to lay measurement points. Total updates live.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // AR viewport
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val boxW = constraints.maxWidth.toFloat()
                        val boxH = constraints.maxHeight.toFloat()
                        val bmp = previewBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "AR view",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryGreen)
                            }
                        }
                        // Tap layer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        pendingTaps = pendingTaps + Offset(offset.x / boxW, offset.y / boxH)
                                        statusMessage = null
                                    }
                                }
                        )
                        // Points + polyline overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pts = points.map { Offset(it.fracX * size.width, it.fracY * size.height) }
                            pts.zipWithNext().forEach { (a, b) ->
                                drawLine(Color(0xFF3B7AE8), a, b, strokeWidth = 5f)
                            }
                            pts.forEachIndexed { i, p ->
                                drawCircle(Color(0xFF3B7AE8), radius = 14f, center = p)
                                drawCircle(Color.White, radius = 6f, center = p)
                            }
                        }
                    }

                    statusMessage?.let { Text(it, color = StatusPending, style = MaterialTheme.typography.labelSmall) }

                    // Readout
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Total: ${String.format("%.1f", totalFeet)} ft",
                                style = MaterialTheme.typography.titleLarge,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${points.size} point${if (points.size == 1) "" else "s"} placed · ${points.zipWithNext().size} segment(s)" +
                                    if (points.size >= 2) " · last: ${String.format("%.1f", segmentFeet(points.last(), points[points.size - 2]))} ft" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                                clipboard.setText(AnnotatedString("Linear footage: ${String.format("%.1f", totalFeet)} ft (${points.size} points, AR measured)"))
                            },
                            enabled = points.size >= 2,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, color = TextPrimary)
            content()
        }
    }
}

private fun segmentFeet(a: MeasurePoint, b: MeasurePoint): Double {
    val d = sqrt(
        ((a.worldX - b.worldX) * (a.worldX - b.worldX) +
            (a.worldY - b.worldY) * (a.worldY - b.worldY) +
            (a.worldZ - b.worldZ) * (a.worldZ - b.worldZ)).toDouble()
    )
    return d * 3.28084
}

private fun yuvToBitmap(image: android.media.Image, rotationDegrees: Int): Bitmap {
    val nv21 = imageToNv21(image)
    val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 55, out)
    val bytes = out.toByteArray()
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    if (rotationDegrees == 0) return bmp
    val m = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}

private fun imageToNv21(image: android.media.Image): ByteArray {
    val width = image.width
    val height = image.height
    val ySize = width * height
    val nv21 = ByteArray(ySize + ySize / 2)

    val yPlane = image.planes[0]
    val yBuffer = yPlane.buffer.duplicate()
    yBuffer.rewind()
    val rowStride = yPlane.rowStride
    if (rowStride == width) {
        yBuffer.get(nv21, 0, ySize)
    } else {
        val row = ByteArray(rowStride)
        for (r in 0 until height) {
            yBuffer.get(row, 0, rowStride)
            System.arraycopy(row, 0, nv21, r * width, width)
        }
    }

    val vPlane = image.planes[2]
    val uPlane = image.planes[1]
    val vBuffer = vPlane.buffer.duplicate()
    val uBuffer = uPlane.buffer.duplicate()
    vBuffer.rewind()
    uBuffer.rewind()
    val vRowStride = vPlane.rowStride
    val uRowStride = uPlane.rowStride
    val vPixStride = vPlane.pixelStride
    val uPixStride = uPlane.pixelStride
    var pos = ySize
    val uvHeight = height / 2
    val uvWidth = width / 2
    for (r in 0 until uvHeight) {
        for (c in 0 until uvWidth) {
            nv21[pos++] = vBuffer.get(r * vRowStride + c * vPixStride)
            nv21[pos++] = uBuffer.get(r * uRowStride + c * uPixStride)
        }
    }
    return nv21
}
