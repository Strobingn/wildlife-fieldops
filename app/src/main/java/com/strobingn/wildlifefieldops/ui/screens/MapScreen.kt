package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    properties: List<Property> = listOf( // sample
        Property("North Ridge", LatLng(41.5, -74.0)),
        Property("South Meadow", LatLng(41.4, -74.1))
    ),
    onBack: () -> Unit = {}
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.45, -74.05), 12f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Property Mapping") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        })

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            properties.forEach { property ->
                Marker(
                    state = MarkerState(position = property.location),
                    title = property.name,
                    snippet = "Wildlife Property"
                )
                // Add polygon for boundary if available
            }
        }

        // Controls for adding markers, drawing boundaries, offline toggle
        Row(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { /* Add marker logic */ }) { Text("Add Marker") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { /* Draw boundary */ }) { Text("Draw Boundary") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { /* Offline map */ }) { Text("Save Offline") }
        }
    }
}

data class Property(val name: String, val location: LatLng)