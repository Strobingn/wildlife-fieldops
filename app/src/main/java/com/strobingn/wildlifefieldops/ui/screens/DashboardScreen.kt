package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.DashboardViewModel

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
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard", color = colors.onBackground, fontWeight = FontWeight.Bold)
                        Text("Wildlife Field Ops", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Open menu", tint = colors.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAI) {
                        Icon(Icons.Default.AutoAwesome, "AI Assistant", tint = colors.onBackground)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.NotificationsNone, "Notifications", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToJobForm,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(50)
            ) { Icon(Icons.Default.Add, "New job") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionTitle("Overview") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(Icons.Default.WorkOutline, stats.todayJobs.toString(), "Today's Jobs", Modifier.weight(1f))
                    MetricCard(Icons.Default.CheckCircleOutline, stats.completedJobs.toString(), "Completed", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(Icons.Default.Schedule, stats.inProgressJobs.toString(), "In Progress", Modifier.weight(1f))
                    MetricCard(Icons.Default.AttachMoney, "$${"%,.0f".format(stats.totalRevenue)}", "Revenue", Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Upcoming Jobs")
                    TextButton(onClick = onNavigateToJobs) { Text("View All") }
                }
            }
            if (recentJobs.isEmpty()) {
                item { V1Card { Text("No scheduled jobs", color = colors.onSurfaceVariant) } }
            } else {
                items(recentJobs.take(4)) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToJobDetail(job.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceVariant,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Pets, null, tint = colors.primary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(job.title, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                                if (job.customerName.isNotBlank()) {
                                    Text(job.customerName, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(job.status.name.replace('_', ' '), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = colors.onSurfaceVariant)
                        }
                    }
                }
            }
            item { SectionTitle("Quick Access") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Default.Search, "Inspections", onNavigateToInspections, Modifier.weight(1f))
                    QuickAction(Icons.Default.CalendarMonth, "Schedule", onNavigateToSchedule, Modifier.weight(1f))
                    QuickAction(Icons.Default.People, "Customers", onNavigateToCustomers, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Default.Map, "Map", onNavigateToMap, Modifier.weight(1f))
                    QuickAction(Icons.Default.AutoAwesome, "AI", onNavigateToAI, Modifier.weight(1f))
                    QuickAction(Icons.Default.Settings, "Settings", onNavigateToSettings, Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(84.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun MetricCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, color = colors.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = colors.primary)
            Text(label, color = colors.onSurface, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun V1Card(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
