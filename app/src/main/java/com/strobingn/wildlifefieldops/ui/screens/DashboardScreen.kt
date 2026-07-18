package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val recentJobs by viewModel.recentJobs.collectAsState()
    val reminders by viewModel.pendingReminders.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wildlife Field App", fontWeight = FontWeight.Bold)
                        Text("Operations overview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Open menu") } },
                actions = {
                    IconButton(onClick = onNavigateToAI) { Icon(Icons.Default.AutoAwesome, "AI Assistant") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToJobForm,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New job") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardMetric("Active", stats.inProgressJobs.toString(), Icons.Default.Construction, Modifier.weight(1f))
                    DashboardMetric("Pending", stats.pendingJobs.toString(), Icons.Default.Schedule, Modifier.weight(1f))
                    DashboardMetric("Revenue", NumberFormat.getCurrencyInstance().format(stats.totalRevenue), Icons.Default.Payments, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Jobs", Icons.Default.Work, onNavigateToJobs, Modifier.weight(1f))
                    QuickAction("Inspections", Icons.Default.FactCheck, onNavigateToInspections, Modifier.weight(1f))
                    QuickAction("Schedule", Icons.Default.CalendarMonth, onNavigateToSchedule, Modifier.weight(1f))
                    QuickAction("Map", Icons.Default.Map, onNavigateToMap, Modifier.weight(1f))
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Field summary", fontWeight = FontWeight.SemiBold)
                            Text("${stats.todayJobs} jobs today · ${stats.overdueJobs} overdue · ${stats.followUpRequired} follow-ups", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        if (reminders.isNotEmpty()) AssistChip(onClick = onNavigateToSchedule, label = { Text("${reminders.size} reminders") })
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent jobs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onNavigateToJobs) { Text("View all") }
                }
            }
            if (recentJobs.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WorkOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No jobs yet", fontWeight = FontWeight.SemiBold)
                            Text("Create your first job to begin field tracking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(recentJobs, key = { it.id }) { job ->
                    RecentJobCard(job = job, onClick = { onNavigateToJobDetail(job.id) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RecentJobCard(job: Job, onClick: () -> Unit) {
    val date = job.scheduledDate ?: job.createdAt
    val dateText = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(date))
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(0.dp)) {
            Box(Modifier.width(6.dp).height(164.dp).background(MaterialTheme.colorScheme.primary))
            Column(Modifier.weight(1f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(job.title.ifBlank { job.type }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (job.customerName.isNotBlank()) Text(job.customerName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    StatusPill(job.status.name.replace('_', ' '))
                }
                JobInfoRow(Icons.Default.LocationOn, job.address.ifBlank { "No address entered" })
                JobInfoRow(Icons.Default.CalendarToday, "Start: $dateText")
                JobInfoRow(Icons.Default.Payments, "Total: ${NumberFormat.getCurrencyInstance().format(if (job.actualCost > 0) job.actualCost else job.estimatedValue)}${if (job.actualCost <= 0) " est." else ""}")
            }
        }
    }
}

@Composable
private fun JobInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusPill(status: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
