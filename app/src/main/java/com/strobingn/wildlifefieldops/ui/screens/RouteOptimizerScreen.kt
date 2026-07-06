package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.strobingn.wildlifefieldops.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteOptimizerScreen(
    onBack: () -> Unit
) {
    var startLocation by remember { mutableStateOf("") }
    var routeStops by remember { mutableStateOf(listOf(
        RouteStop("1", "123 Main St, Middletown, NY", LatLng(41.45, -74.05), "Raccoon removal"),
        RouteStop("2", "456 Oak Ave, Goshen, NY", LatLng(41.40, -74.32), "Squirrel inspection"),
        RouteStop("3", "789 Pine Rd, Warwick, NY", LatLng(41.26, -74.36), "Bat exclusion"),
        RouteStop("4", "321 Elm Dr, Monroe, NY", LatLng(41.33, -74.19), "Follow-up visit")
    )) }
    var isOptimized by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.40, -74.20), 10f)
    }

    val totalDistance = calculateTotalDistance(routeStops)
    val estimatedTime = (totalDistance / 40.0 * 60).toInt() // 40 mph avg

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route Optimizer", color = TextPrimary) },
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
            // Map
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                cameraPositionState = cameraPositionState
            ) {
                routeStops.forEachIndexed { index, stop ->
                    Marker(
                        state = MarkerState(position = stop.latLng),
                        title = "${index + 1}. ${stop.address}",
                        snippet = stop.jobDescription
                    )
                }

                if (routeStops.size > 1) {
                    val points = routeStops.map { it.latLng }
                    Polyline(
                        points = points,
                        color = PrimaryGreen,
                        width = 6f
                    )
                }
            }

            // Route Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RouteStat("${routeStops.size}", "Stops", AccentBlue)
                    RouteStat("${String.format("%.1f", totalDistance)}", "Miles", PrimaryGreen)
                    RouteStat("$estimatedTime", "Min", AccentPurple)
                }
            }

            // Start Location
            OutlinedTextField(
                value = startLocation,
                onValueChange = { startLocation = it },
                label = { Text("Starting Location") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryGreen) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = BackgroundCard,
                    unfocusedContainerColor = BackgroundCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!isOptimized) {
                            // Simple nearest-neighbor optimization
                            routeStops = optimizeRoute(routeStops)
                            isOptimized = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isOptimized) AccentBlue else PrimaryGreen, contentColor = if (isOptimized) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (isOptimized) Icons.Default.Check else Icons.Default.Route, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isOptimized) "Optimized" else "Optimize Route", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        routeStops = routeStops.shuffled()
                        isOptimized = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                }
            }

            Text(
                "Route Stops",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(routeStops, key = { it.id }) { stop ->
                    val index = routeStops.indexOf(stop)
                    RouteStopCard(stop = stop, index = index + 1, onRemove = {
                        routeStops = routeStops.filter { it.id != stop.id }
                    })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

data class RouteStop(
    val id: String,
    val address: String,
    val latLng: LatLng,
    val jobDescription: String
)

private fun calculateTotalDistance(stops: List<RouteStop>): Double {
    if (stops.size < 2) return 0.0
    var total = 0.0
    for (i in 0 until stops.size - 1) {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            stops[i].latLng.latitude, stops[i].latLng.longitude,
            stops[i + 1].latLng.latitude, stops[i + 1].latLng.longitude,
            results
        )
        total += results[0] * 0.000621371 // meters to miles
    }
    return total
}

private fun optimizeRoute(stops: List<RouteStop>): List<RouteStop> {
    if (stops.size < 3) return stops
    val mutable = stops.toMutableList()
    val optimized = mutableListOf<RouteStop>(mutable.removeFirst())
    while (mutable.isNotEmpty()) {
        val last = optimized.last()
        val nearest = mutable.minByOrNull { stop ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                last.latLng.latitude, last.latLng.longitude,
                stop.latLng.latitude, stop.latLng.longitude,
                results
            )
            results[0]
        }
        nearest?.let {
            optimized.add(it)
            mutable.remove(it)
        }
    }
    return optimized
}

@Composable
private fun RouteStat(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun RouteStopCard(stop: RouteStop, index: Int, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PrimaryGreen.copy(alpha = 0.2f)
            ) {
                Text(
                    index.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.address, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text(stop.jobDescription, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = ErrorRed.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
