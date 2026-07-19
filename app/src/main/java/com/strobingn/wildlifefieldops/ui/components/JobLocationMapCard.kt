package com.strobingn.wildlifefieldops.ui.components

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.strobingn.wildlifefieldops.ui.theme.BackgroundCard
import com.strobingn.wildlifefieldops.ui.theme.PrimaryGreen
import com.strobingn.wildlifefieldops.ui.theme.TextPrimary
import com.strobingn.wildlifefieldops.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class LocationResolveState {
    Idle,
    Loading,
    Ready,
    Failed,
    Empty
}

/**
 * Inline Google Maps card for job house location on Job Detail.
 * Uses stored lat/lng when present; otherwise forward-geocodes [address].
 */
@Composable
fun JobLocationMapCard(
    address: String,
    latitude: Double?,
    longitude: Double?,
    customerLabel: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var resolveState by remember { mutableStateOf(LocationResolveState.Idle) }
    var resolvedPoint by remember { mutableStateOf<LatLng?>(null) }

    val mapHeight by animateDpAsState(
        targetValue = if (expanded) 320.dp else 180.dp,
        label = "jobMapHeight"
    )

    LaunchedEffect(latitude, longitude, address) {
        when {
            latitude != null && longitude != null -> {
                resolvedPoint = LatLng(latitude, longitude)
                resolveState = LocationResolveState.Ready
            }
            address.isNotBlank() -> {
                resolveState = LocationResolveState.Loading
                resolvedPoint = null
                val geocoded = geocodeAddress(context, address)
                if (geocoded != null) {
                    resolvedPoint = geocoded
                    resolveState = LocationResolveState.Ready
                } else {
                    resolveState = LocationResolveState.Failed
                }
            }
            else -> {
                resolvedPoint = null
                resolveState = LocationResolveState.Empty
            }
        }
    }

    // Default camera; updated when point resolves. Do NOT call CameraUpdateFactory
    // until the Maps SDK has initialized (see onMapLoaded) — that NPE crashes Job Detail.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.45, -74.05), 12f)
    }
    var mapLoaded by remember { mutableStateOf(false) }

    // Safe: assigning CameraPosition does not require CameraUpdateFactory.
    LaunchedEffect(resolvedPoint, expanded) {
        val point = resolvedPoint ?: return@LaunchedEffect
        val zoom = if (expanded) 17.5f else 16.5f
        cameraPositionState.position = CameraPosition.fromLatLngZoom(point, zoom)
    }

    // Optional smooth animate only after GoogleMap reports loaded (SDK ready).
    LaunchedEffect(resolvedPoint, expanded, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        val point = resolvedPoint ?: return@LaunchedEffect
        val zoom = if (expanded) 17.5f else 16.5f
        runCatching {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(point, zoom))
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "House location",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (resolveState) {
                LocationResolveState.Empty -> {
                    EmptyLocationBody("No location on this job")
                }
                LocationResolveState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mapHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = PrimaryGreen,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Finding house on map…", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AddressCaption(address)
                }
                LocationResolveState.Failed -> {
                    EmptyLocationBody("Couldn’t place a pin for this address")
                    AddressCaption(address)
                    OpenInMapsButton(
                        onClick = {
                            openInGoogleMaps(
                                context = context,
                                latitude = null,
                                longitude = null,
                                address = address,
                                label = customerLabel.ifBlank { address }
                            )
                        }
                    )
                }
                LocationResolveState.Ready, LocationResolveState.Idle -> {
                    val point = resolvedPoint
                    if (point == null) {
                        EmptyLocationBody("No location on this job")
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(mapHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { expanded = !expanded }
                        ) {
                            val markerState = remember(point.latitude, point.longitude) {
                                MarkerState(position = point)
                            }
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                properties = MapProperties(
                                    isMyLocationEnabled = false,
                                    mapType = MapType.HYBRID
                                ),
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = expanded,
                                    myLocationButtonEnabled = false,
                                    compassEnabled = expanded,
                                    mapToolbarEnabled = false,
                                    scrollGesturesEnabled = expanded,
                                    zoomGesturesEnabled = expanded,
                                    rotationGesturesEnabled = false,
                                    tiltGesturesEnabled = false
                                ),
                                onMapLoaded = { mapLoaded = true },
                                onMapClick = { expanded = !expanded }
                            ) {
                                Marker(
                                    state = markerState,
                                    title = customerLabel.ifBlank { "House" },
                                    snippet = address.ifBlank { null },
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                            // Tap affordance when collapsed
                            if (!expanded) {
                                Text(
                                    "Tap to expand",
                                    color = TextPrimary.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        AddressCaption(
                            if (address.isNotBlank()) address
                            else "Location pin only"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OpenInMapsButton(
                                onClick = {
                                    openInGoogleMaps(
                                        context = context,
                                        latitude = point.latitude,
                                        longitude = point.longitude,
                                        address = address,
                                        label = customerLabel.ifBlank { address }
                                    )
                                },
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (expanded) {
                                TextButton(onClick = { expanded = false }) {
                                    Text("Collapse", color = PrimaryGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressCaption(text: String) {
    if (text.isBlank()) return
    Spacer(modifier = Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
}

@Composable
private fun EmptyLocationBody(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Map, contentDescription = null, tint = TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OpenInMapsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Open in Google Maps", color = PrimaryGreen)
    }
}

@Suppress("DEPRECATION")
private suspend fun geocodeAddress(context: android.content.Context, address: String): LatLng? =
    withContext(Dispatchers.IO) {
        if (address.isBlank() || !Geocoder.isPresent()) return@withContext null
        runCatching {
            Geocoder(context, Locale.getDefault())
                .getFromLocationName(address, 1)
                ?.firstOrNull()
                ?.let { LatLng(it.latitude, it.longitude) }
        }.getOrNull()
    }

private fun openInGoogleMaps(
    context: android.content.Context,
    latitude: Double?,
    longitude: Double?,
    address: String,
    label: String
) {
    val uri = when {
        latitude != null && longitude != null -> {
            val qLabel = Uri.encode(label.ifBlank { "$latitude,$longitude" })
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($qLabel)")
        }
        address.isNotBlank() -> {
            Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        }
        else -> null
    }
    if (uri == null) {
        Toast.makeText(context, "No location to open", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
    }
}
