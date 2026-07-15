package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ui.theme.BackgroundCard
import com.strobingn.wildlifefieldops.ui.theme.BackgroundDark
import com.strobingn.wildlifefieldops.ui.theme.PrimaryGreen
import com.strobingn.wildlifefieldops.ui.theme.TextPrimary
import com.strobingn.wildlifefieldops.ui.theme.TextSecondary
import com.strobingn.wildlifefieldops.ui.theme.TextTertiary
import com.strobingn.wildlifefieldops.ui.viewmodel.DashboardViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.WeatherUiState
import com.strobingn.wildlifefieldops.ui.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToJobs: () -> Unit,
    onNavigateToInspections: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToJobDetail: (String) -> Unit,
    onNavigateToJobForm: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAI: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    weatherViewModel: WeatherViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val recentJobs by viewModel.recentJobs.collectAsState()
    val reminders by viewModel.pendingReminders.collectAsState()
    val weather by weatherViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wildlife FieldOps", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAI) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Assistant")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToJobForm, containerColor = PrimaryGreen) {
                Icon(Icons.Default.Add, contentDescription = "New Job")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { WeatherCard(weather = weather, onRefresh = weatherViewModel::refresh) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardMetric("Active", stats.inProgressJobs.toString(), Modifier.weight(1f))
                    DashboardMetric("Pending", stats.pendingJobs.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardMetric("Completed", stats.completedJobs.toString(), Modifier.weight(1f))
                    DashboardMetric("Revenue", "$${"%.2f".format(stats.totalRevenue)}", Modifier.weight(1f))
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Overview", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Jobs today: ${stats.todayJobs}", color = TextSecondary)
                        Text("Overdue: ${stats.overdueJobs}", color = TextSecondary)
                        Text("Customers: ${stats.totalCustomers}", color = TextSecondary)
                        Text("Inspections: ${stats.totalInspections}", color = TextSecondary)
                        Text("Follow-ups: ${stats.followUpRequired}", color = TextSecondary)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNavigateToJobs, modifier = Modifier.weight(1f)) { Text("Jobs") }
                    Button(onClick = onNavigateToCustomers, modifier = Modifier.weight(1f)) { Text("Customers") }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNavigateToInspections, modifier = Modifier.weight(1f)) { Text("Inspections") }
                    Button(onClick = onNavigateToSchedule, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Text("Schedule")
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNavigateToMap, modifier = Modifier.weight(1f)) { Text("Map") }
                    Button(onClick = onNavigateToAI, modifier = Modifier.weight(1f)) { Text("AI") }
                }
            }
            item { Text("Recent Jobs", style = MaterialTheme.typography.titleMedium, color = TextPrimary) }
            if (recentJobs.isEmpty()) {
                item { Text("No jobs yet", color = TextSecondary) }
            } else {
                items(recentJobs) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                        onClick = { onNavigateToJobDetail(job.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(job.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            if (job.customerName.isNotBlank()) Text(job.customerName, color = TextSecondary)
                            Text(job.status.name.replace('_', ' '), color = TextSecondary)
                        }
                    }
                }
            }
            if (reminders.isNotEmpty()) {
                item { Text("Pending reminders: ${reminders.size}", color = TextSecondary) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherUiState, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryGreen)
                    Column {
                        Text("Field Weather", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(weather.locationLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                IconButton(onClick = onRefresh, enabled = !weather.loading) {
                    if (weather.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "Refresh weather", tint = PrimaryGreen)
                }
            }

            if (weather.temperatureF != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("${weather.temperatureF}°F", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(weather.condition, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("High ${weather.highF ?: "–"}°  Low ${weather.lowF ?: "–"}°", color = TextSecondary)
                        Text("Feels like ${weather.apparentTemperatureF ?: "–"}°", color = TextSecondary)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeatherDetail(Icons.Default.WaterDrop, "Rain", "${weather.precipitationChance ?: 0}%")
                    WeatherDetail(Icons.Default.Air, "Wind", "${weather.windMph ?: 0} mph")
                    WeatherDetail(Icons.Default.Thermostat, "Range", "${weather.lowF ?: "–"}–${weather.highF ?: "–"}°")
                }
            } else {
                Text(weather.error ?: weather.condition, color = if (weather.error == null) TextSecondary else MaterialTheme.colorScheme.error)
            }
            Text("Weather data: Open-Meteo", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

@Composable
private fun WeatherDetail(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = PrimaryGreen)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(value, color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
