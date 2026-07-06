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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.DashboardViewModel
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
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val recentJobs by viewModel.recentJobs.collectAsState()
    val reminders by viewModel.pendingReminders.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wildlife FieldOps", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToAI) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Assistant", tint = AccentPurple)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToJobForm,
                containerColor = PrimaryGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Job")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Cards Row
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Active",
                        value = stats.inProgressJobs.toString(),
                        icon = Icons.Default.PlayCircle,
                        color = AccentBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToJobs
                    )
                    StatCard(
                        title = "Pending",
                        value = stats.pendingJobs.toString(),
                        icon = Icons.Default.Schedule,
                        color = StatusPending,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToJobs
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Completed",
                        value = stats.completedJobs.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToJobs
                    )
                    StatCard(
                        title = "Revenue",
                        value = "$${String.format("%.0f", stats.totalRevenue)}",
                        icon = Icons.Default.AttachMoney,
                        color = PrimaryGreen,
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    )
                }
            }

            // Today's Overview
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OverviewCard(
                    title = "Today's Overview",
                    items = listOf(
                        OverviewItem("Jobs Today", stats.todayJobs.toString(), Icons.Default.Work, AccentBlue),
                        OverviewItem("Overdue", stats.overdueJobs.toString(), Icons.Default.Warning, StatusUrgent),
                        OverviewItem("Customers", stats.totalCustomers.toString(), Icons.Default.People, AccentPurple),
                        OverviewItem("Inspections", stats.totalInspections.toString(), Icons.Default.Search, AccentCyan),
                        OverviewItem("Follow-ups", stats.followUpRequired.toString(), Icons.Default.FollowTheSigns, AccentOrange),
                        OverviewItem("Revenue", "$${String.format("%.0f", stats.totalRevenue)}", Icons.Default.AttachMoney, PrimaryGreen)
                    )
                )
            }

            // Quick Actions
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton("New Job", Icons.Default.AddBox, PrimaryGreen, Modifier.weight(1f), onNavigateToJobForm)
                    QuickActionButton("Customers", Icons.Default.People, AccentPurple, Modifier.weight(1f), onNavigateToCustomers)
                    QuickActionButton("Map", Icons.Default.Map, AccentBlue, Modifier.weight(1f), onNavigateToMap)
                    QuickActionButton("Schedule", Icons.Default.CalendarMonth, AccentOrange, Modifier.weight(1f), onNavigateToSchedule)
                }
            }

            // Recent Jobs
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Jobs", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    TextButton(onClick = onNavigateToJobs) {
                        Text("View All", color = PrimaryGreen)
                    }
                }
            }

            if (recentJobs.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No jobs yet. Create your first job!",
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(recentJobs) { job ->
                    JobCard(job = job, onClick = { onNavigateToJobDetail(job.id) })
                }
            }

            // Pending Reminders
            if (reminders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reminders", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(reminders) { reminder ->
                    ReminderCard(reminder = reminder)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OverviewCard(title: String, items: List<OverviewItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.chunked(2).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { item ->
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(item.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(item.value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class OverviewItem(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundCard)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun JobCard(job: Job, onClick: () -> Unit) {
    val statusColor = when (job.status) {
        JobStatus.PENDING -> StatusPending
        JobStatus.IN_PROGRESS -> AccentBlue
        JobStatus.COMPLETED -> SuccessGreen
        JobStatus.CANCELLED -> ErrorRed
        JobStatus.INVOICED -> AccentPurple
        JobStatus.PAID -> PrimaryGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    job.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (job.customerName.isNotBlank()) {
                    Text(
                        job.customerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (job.address.isNotBlank()) {
                    Text(
                        job.address,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    job.status.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: com.strobingn.wildlifefieldops.data.model.Reminder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text(
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(reminder.dueDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
