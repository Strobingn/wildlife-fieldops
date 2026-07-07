package com.strobingn.wildlifefieldops.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.MapProperty
import com.strobingn.wildlifefieldops.ui.viewmodel.MapViewModel
import com.google.maps.android.compose.MapType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onNavigateToJobDetail: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val properties by viewModel.filteredProperties.collectAsState()
    val isDrawing by viewModel.isDrawingBoundary.collectAsState()
    val boundaryPoints by viewModel.boundaryPoints.collectAsState()
    val context = LocalContext.current

    var showSearch by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.45, -74.05), 12f)
    }

    Scaffold(
        topBar = {
            if (!showSearch) {
                TopAppBar(
                    title = { Text("Property Map", color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                        }
                        IconButton(onClick = { showControls = !showControls }) {
                            Icon(
                                if (showControls) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = "Toggle Controls",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark.copy(alpha = 0.9f))
                )
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = MapType.HYBRID
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = true,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                ),
                onMapClick = { latLng ->
                    if (isDrawing) {
                        viewModel.addBoundaryPoint(latLng)
                    }
                }
            ) {
                // Property markers
                properties.forEach { property ->
                    val markerColor = when (property.status) {
                        JobStatus.PENDING -> BitmapDescriptorFactory.HUE_YELLOW
                        JobStatus.IN_PROGRESS -> BitmapDescriptorFactory.HUE_BLUE
                        JobStatus.COMPLETED -> BitmapDescriptorFactory.HUE_GREEN
                        JobStatus.CANCELLED -> BitmapDescriptorFactory.HUE_RED
                        JobStatus.INVOICED -> BitmapDescriptorFactory.HUE_VIOLET
                        JobStatus.PAID -> BitmapDescriptorFactory.HUE_GREEN
                    }

                    Marker(
                        state = MarkerState(position = LatLng(property.latitude, property.longitude)),
                        title = property.name,
                        snippet = "${property.address} (${property.type})",
                        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                        onClick = {
                            onNavigateToJobDetail(property.id)
                            true
                        }
                    )
                }

                // Boundary polygon
                if (boundaryPoints.size > 2) {
                    Polygon(
                        points = boundaryPoints,
                        fillColor = PrimaryGreen.copy(alpha = 0.15f),
                        strokeColor = PrimaryGreen,
                        strokeWidth = 3f
                    )
                }

                // Boundary points markers
                if (isDrawing) {
                    boundaryPoints.forEachIndexed { index, point ->
                        Marker(
                            state = MarkerState(position = point),
                            title = "Point ${index + 1}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        )
                    }
                }
            }

            // Search overlay
            if (showSearch) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.padding(start = 8.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text("Search properties...", color = TextTertiary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, capitalization = KeyboardCapitalization.Words)
                        )
                        IconButton(onClick = {
                            viewModel.setSearchQuery("")
                            showSearch = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }
            }

            // Bottom Controls
            if (showControls) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Property count
                        Text(
                            "${properties.size} properties shown",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MapControlButton(
                                label = if (isDrawing) "Drawing..." else "Boundary",
                                icon = if (isDrawing) Icons.Default.Edit else Icons.Default.Gesture,
                                active = isDrawing,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.toggleDrawingMode() }
                            )

                            MapControlButton(
                                label = "Save",
                                icon = Icons.Default.Save,
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.saveBoundary() }
                            )

                            MapControlButton(
                                label = "Clear",
                                icon = Icons.Default.ClearAll,
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.clearBoundary() }
                            )

                            MapControlButton(
                                label = "Snapshot",
                                icon = Icons.Default.CameraAlt,
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    // Save map snapshot functionality
                                }
                            )
                        }

                        // Legend
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendDot("Pending", StatusPending)
                            LegendDot("Active", AccentBlue)
                            LegendDot("Done", SuccessGreen)
                            LegendDot("Cancelled", ErrorRed)
                        }
                    }
                }
            }

            // Drawing mode indicator
            if (isDrawing) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (showSearch) 80.dp else 16.dp)
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusPending.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Tap map to add boundary points (${boundaryPoints.size} set)",
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (active) PrimaryGreen.copy(alpha = 0.2f) else SurfaceVariant
    val contentColor = if (active) PrimaryGreen else TextSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}
