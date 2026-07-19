package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.RouteOptimizerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteOptimizerScreen(
    onBack: () -> Unit,
    viewModel: RouteOptimizerViewModel = hiltViewModel()
) {
    val jobs by viewModel.routableJobs.collectAsState()
    var orderedJobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var optimized by remember { mutableStateOf(false) }

    LaunchedEffect(jobs.map { it.id }) {
        orderedJobs = jobs.sortedWith(
            compareBy<Job> { it.scheduledDate ?: Long.MAX_VALUE }
                .thenByDescending { it.priority.ordinal }
        )
        optimized = false
    }

    val firstPoint = orderedJobs.firstOrNull()?.let { LatLng(it.latitude!!, it.longitude!!) }
        ?: LatLng(41.43, -74.04)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(firstPoint, 10f)
    }

    LaunchedEffect(firstPoint) {
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(firstPoint, 10f))
    }

    val totalMiles = calculateDistanceMiles(orderedJobs)
    val estimatedMinutes = (totalMiles / 35.0 * 60.0).toInt()

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
                .imePadding()
        ) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                cameraPositionState = cameraPositionState
            ) {
                orderedJobs.forEachIndexed { index, job ->
                    val point = LatLng(job.latitude!!, job.longitude!!)
                    Marker(
                        state = MarkerState(point),
                        title = "${index + 1}. ${job.customerName.ifBlank { job.title }}",
                        snippet = job.address
                    )
                }
                if (orderedJobs.size > 1) {
                    Polyline(
                        points = orderedJobs.map { LatLng(it.latitude!!, it.longitude!!) },
                        color = PrimaryGreen,
                        width = 7f
                    )
                }
            }

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
                    RouteMetric(orderedJobs.size.toString(), "Stops", AccentBlue)
                    RouteMetric(String.format("%.1f", totalMiles), "Miles", PrimaryGreen)
                    RouteMetric(estimatedMinutes.toString(), "Minutes", AccentPurple)
                }
            }

            Button(
                onClick = {
                    orderedJobs = viewModel.optimize(orderedJobs)
                    optimized = true
                },
                enabled = orderedJobs.size > 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (optimized) AccentBlue else PrimaryGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (optimized) Icons.Default.Check else Icons.Default.Route, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (optimized) "Route Optimized" else "Optimize Route", fontWeight = FontWeight.Bold)
            }

            if (orderedJobs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No routable jobs", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        "Open jobs must have latitude and longitude saved before they can be routed.",
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(orderedJobs, key = { _, job -> job.id }) { index, job ->
                        RouteJobCard(index + 1, job)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteMetric(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RouteJobCard(position: Int, job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = PrimaryGreen.copy(alpha = 0.18f)) {
                Text(
                    position.toString(),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(job.customerName.ifBlank { job.title }, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(job.address, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(job.type, color = PrimaryGreen, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun calculateDistanceMiles(jobs: List<Job>): Double {
    if (jobs.size < 2) return 0.0
    return jobs.zipWithNext().sumOf { (a, b) ->
        val result = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude ?: 0.0,
            a.longitude ?: 0.0,
            b.latitude ?: 0.0,
            b.longitude ?: 0.0,
            result
        )
        result[0] * 0.000621371
    }
}
