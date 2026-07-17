package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.analytics.AnalyticsSnapshot
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.ui.viewmodel.AnalyticsViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    onOpenJobs: (JobStatus?, String?) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val snapshot by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            snapshot.totalJobs == 0 && snapshot.invoicedRevenue == 0.0 -> EmptyAnalytics(
                modifier = Modifier.fillMaxSize().padding(padding),
                onCreateOrViewJobs = { onOpenJobs(null, null) }
            )

            else -> AnalyticsContent(
                snapshot = snapshot,
                onOpenJobs = onOpenJobs,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun AnalyticsContent(
    snapshot: AnalyticsSnapshot,
    onOpenJobs: (JobStatus?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionTitle("Job status") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("All jobs", snapshot.totalJobs.toString(), Modifier.weight(1f)) { onOpenJobs(null, null) }
                MetricCard("Pending", snapshot.pendingJobs.toString(), Modifier.weight(1f)) { onOpenJobs(JobStatus.PENDING, null) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Active", snapshot.activeJobs.toString(), Modifier.weight(1f)) { onOpenJobs(JobStatus.IN_PROGRESS, null) }
                MetricCard("Completed", snapshot.completedJobs.toString(), Modifier.weight(1f)) { onOpenJobs(JobStatus.COMPLETED, null) }
            }
        }

        item { SectionTitle("Revenue and cost") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard("Estimated value", snapshot.estimatedJobValue, Modifier.weight(1f))
                MoneyCard("Actual cost", snapshot.actualJobCost, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard("Invoiced", snapshot.invoicedRevenue, Modifier.weight(1f))
                MoneyCard("Collected", snapshot.collectedRevenue, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard("Outstanding", snapshot.outstandingBalance, Modifier.weight(1f))
                MetricCard(
                    "Gross margin",
                    "${money(snapshot.grossMargin)}\n${String.format("%.1f%%", snapshot.grossMarginPercent)}",
                    Modifier.weight(1f)
                )
            }
        }

        item { SectionTitle("Service types") }
        if (snapshot.jobsByServiceType.isEmpty()) {
            item { Text("No service-type data yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(snapshot.jobsByServiceType.toList(), key = { it.first }) { (type, count) ->
                ListItem(
                    headlineContent = { Text(type) },
                    trailingContent = { Text(count.toString(), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.clickable { onOpenJobs(null, type) }
                )
                HorizontalDivider()
            }
        }

        item { SectionTitle("Sync health") }
        item {
            Text(
                "Unsynced jobs: ${snapshot.unsyncedJobs}  •  Unsynced invoices: ${snapshot.unsyncedInvoices}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    ElevatedCard(modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MoneyCard(label: String, value: Double, modifier: Modifier = Modifier) =
    MetricCard(label, money(value), modifier)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyAnalytics(modifier: Modifier = Modifier, onCreateOrViewJobs: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("No analytics yet", style = MaterialTheme.typography.titleLarge)
        Text("Create jobs and invoices to populate live business metrics.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onCreateOrViewJobs) {
            Icon(Icons.Default.WorkOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open jobs")
        }
    }
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance().format(value)
