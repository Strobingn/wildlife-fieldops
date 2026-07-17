package com.strobingn.wildlifefieldops.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.strobingn.wildlifefieldops.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPSScreen(
    onNavigateToMap: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val hasLocationPermission = remember(context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var trackingEnabled by remember { mutableStateOf(false) }
    var gpsAccuracy by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(0f) }
    var altitude by remember { mutableDoubleStateOf(0.0) }
    var satellites by remember { mutableIntStateOf(0) }
    var totalDistance by remember { mutableDoubleStateOf(0.0) }
    var trackPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var elapsedTime by remember { mutableLongStateOf(0L) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.45, -74.05), 15f)
    }

    DisposableEffect(trackingEnabled, hasLocationPermission, appContext) {
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var registeredListener: LocationListener? = null

        if (trackingEnabled && hasLocationPermission && locationManager != null) {
            val gpsAvailable = runCatching {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }.getOrDefault(false)

            if (gpsAvailable) {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val newLatLng = LatLng(location.latitude, location.longitude)

                        currentLocation?.let { previous ->
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                previous.latitude,
                                previous.longitude,
                                newLatLng.latitude,
                                newLatLng.longitude,
                                results
                            )
                            totalDistance += results[0].toDouble()
                        }

                        currentLocation = newLatLng
                        gpsAccuracy = location.accuracy
                        speed = location.speed * 2.23694f
                        altitude = location.altitude
                        trackPoints = trackPoints + newLatLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(newLatLng, 17f)
                    }

                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) = Unit
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1_000L,
                            1f,
                            ContextCompat.getMainExecutor(context),
                            listener
                        )
                    } else {
                        val mainLooper = Looper.getMainLooper()
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1_000L,
                            1f,
                            listener,
                            mainLooper
                        )
                    }
                    registeredListener = listener
                } catch (error: SecurityException) {
                    Log.e("GPSScreen", "Location permission was revoked", error)
                    trackingEnabled = false
                } catch (error: RuntimeException) {
                    Log.e("GPSScreen", "Unable to register GPS updates", error)
                    trackingEnabled = false
                }
            } else {
                trackingEnabled = false
            }
        }

        onDispose {
            registeredListener?.let { listener ->
                runCatching { locationManager?.removeUpdates(listener) }
                    .onFailure { Log.w("GPSScreen", "Unable to remove GPS updates", it) }
            }
        }
    }

    LaunchedEffect(trackingEnabled) {
        if (trackingEnabled) {
            val startTime = System.currentTimeMillis() - elapsedTime
            while (trackingEnabled) {
                elapsedTime = System.currentTimeMillis() - startTime
                delay(1_000L)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Tracking", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, contentDescription = "Full Map", tint = TextSecondary)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatPill("${String.format("%.1f", speed)} mph", Icons.Default.Speed, AccentBlue, Modifier.weight(1f))
                StatPill("${String.format("%.0f", totalDistance)} ft", Icons.Default.Straighten, PrimaryGreen, Modifier.weight(1f))
                StatPill(formatElapsedTime(elapsedTime), Icons.Default.Timer, AccentPurple, Modifier.weight(1f))
            }

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = hasLocationPermission,
                        compassEnabled = true
                    )
                ) {
                    currentLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Current Location",
                            snippet = "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                        )
                    }

                    if (trackPoints.size > 1) {
                        Polyline(
                            points = trackPoints,
                            color = PrimaryGreen,
                            width = 6f
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (hasLocationPermission) {
                            trackingEnabled = !trackingEnabled
                            if (!trackingEnabled) {
                                elapsedTime = 0L
                                trackPoints = emptyList()
                                totalDistance = 0.0
                            }
                        }
                    },
                    enabled = hasLocationPermission,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (trackingEnabled) ErrorRed else PrimaryGreen,
                        contentColor = if (trackingEnabled) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (trackingEnabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        when {
                            !hasLocationPermission -> "Location Permission Required"
                            trackingEnabled -> "Stop Tracking"
                            else -> "Start Tracking"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (trackPoints.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            trackPoints = emptyList()
                            totalDistance = 0.0
                            elapsedTime = 0L
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundCard)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatElapsedTime(ms: Long): String {
    val seconds = ms / 1_000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    return String.format("%02d:%02d:%02d", hours, minutes % 60L, seconds % 60L)
}
