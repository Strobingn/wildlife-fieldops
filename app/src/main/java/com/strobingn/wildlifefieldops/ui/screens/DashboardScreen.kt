package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

import com.strobingn.wildlifefieldops.ui.viewmodel.DashboardViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardScreen() {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Hudson Valley Weather
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Hudson Valley, NY Weather", style = MaterialTheme.typography.titleLarge)
                Text("Temp: ${state.weatherTemp}°F | ${state.weatherCondition}")
                Button(onClick = { viewModel.refreshWeather() }) {
                    Text("Refresh")
                }
            }
        }

        // Money Out in Open Jobs
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Money Out in Open Jobs: $${state.openJobsTotal}", style = MaterialTheme.typography.titleLarge)
                Text("Pending + In Progress")
            }
        }

        // Other dashboard content...
        Text("Welcome to FieldOps - Wildlife Whisperer LLC")
    }
}
