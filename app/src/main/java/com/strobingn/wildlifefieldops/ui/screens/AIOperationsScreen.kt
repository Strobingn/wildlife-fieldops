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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ai.InventoryForecast
import com.strobingn.wildlifefieldops.ai.OperationsInsight
import com.strobingn.wildlifefieldops.ai.PrioritizedJob
import com.strobingn.wildlifefieldops.ai.PropertyRisk
import com.strobingn.wildlifefieldops.ai.SafetySignal
import com.strobingn.wildlifefieldops.ai.SeasonalForecast
import com.strobingn.wildlifefieldops.ai.operations.AIOperationsEngine.PricingInsight
import com.strobingn.wildlifefieldops.ai.operations.AIOperationsEngine.QualityCheck
import com.strobingn.wildlifefieldops.ui.theme.BackgroundCard
import com.strobingn.wildlifefieldops.ui.theme.BackgroundDark
import com.strobingn.wildlifefieldops.ui.theme.ErrorRed
import com.strobingn.wildlifefieldops.ui.theme.PrimaryGreen
import com.strobingn.wildlifefieldops.ui.theme.StatusPending
import com.strobingn.wildlifefieldops.ui.theme.TextPrimary
import com.strobingn.wildlifefieldops.ui.theme.TextSecondary
import com.strobingn.wildlifefieldops.ui.viewmodel.AIOperationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIOperationsScreen(
    onBack: () -> Unit,
    onNavigateToJob: (String) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: AIOperationsViewModel = hiltViewModel()
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val qualityDashboard by viewModel.qualityDashboard.collectAsState()
    val briefing by viewModel.briefing.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Operations Center", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Live intelligence from jobs, schedules, properties, prices, safety terms, and inventory.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OperationsMetric("Active jobs", snapshot.activeJobs.toString(), Modifier.weight(1f))
                    OperationsMetric("Urgent", snapshot.urgentJobs.toString(), Modifier.weight(1f), ErrorRed)
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OperationsMetric("High-risk sites", snapshot.highRiskProperties.toString(), Modifier.weight(1f), StatusPending)
                    OperationsMetric("Stock alerts", snapshot.inventoryForecasts.size.toString(), Modifier.weight(1f))
                }
            }

            item {
                IntelligenceBriefCard(
                    briefing = briefing,
                    isGenerating = isGenerating,
                    providerLabel = viewModel.providerLabel,
                    configured = viewModel.isLiveAiConfigured,
                    onGenerate = viewModel::generateBriefing
                )
            }

            item { SectionHeader("Smart priority queue", "Risk-ranked from live job records") }
            if (snapshot.prioritizedJobs.isEmpty()) {
                item { EmptySignal("No active jobs to prioritize") }
            } else {
                items(snapshot.prioritizedJobs, key = { it.jobId }) { job ->
                    PriorityJobCard(job = job, onClick = { onNavigateToJob(job.jobId) })
                }
            }

            item { SectionHeader("Seasonal activity forecast", "Seasonal patterns strengthened by your job history") }
            items(snapshot.seasonalForecasts, key = { it.species }) { forecast ->
                ForecastCard(forecast)
            }

            item {
                SectionHeader(
                    title = "Property risk zones",
                    subtitle = "Repeat visits, active work, priority, and damage indicators",
                    actionLabel = "Open map",
                    onAction = onNavigateToMap
                )
            }
            if (snapshot.propertyRisks.isEmpty()) {
                item { EmptySignal("Add job addresses to calculate property risk") }
            } else {
                items(snapshot.propertyRisks, key = { it.address }) { risk ->
                    PropertyRiskCard(risk)
                }
            }

            item { SectionHeader("Safety intelligence", "PPE and procedure signals from active job descriptions") }
            if (snapshot.safetySignals.isEmpty()) {
                item { EmptySignal("No elevated safety keywords in active jobs") }
            } else {
                items(snapshot.safetySignals, key = { it.title }) { signal ->
                    SafetyCard(signal)
                }
            }

            item { SectionHeader("AI quality control", "Missing fields and documentation scored for every job") }
            if (qualityDashboard.qualityChecks.isEmpty()) {
                item { EmptySignal("No jobs available for quality checks") }
            } else {
                items(qualityDashboard.qualityChecks.take(12), key = { "quality-${it.jobId}" }) { check ->
                    QualityCheckCard(check = check, onClick = { onNavigateToJob(check.jobId) })
                }
            }

            item {
                SectionHeader(
                    title = "Inventory forecast",
                    subtitle = "Reorder levels combined with current job demand",
                    actionLabel = "Inventory",
                    onAction = onNavigateToInventory
                )
            }
            if (snapshot.inventoryForecasts.isEmpty()) {
                item { EmptySignal("No low-stock or demand-pressure alerts") }
            } else {
                items(snapshot.inventoryForecasts, key = { it.itemId }) { forecast ->
                    InventoryForecastCard(forecast)
                }
            }

            item { SectionHeader("Business intelligence", "Pricing coverage, estimate variance, and repeat service") }
            items(snapshot.businessInsights, key = { it.title }) { insight ->
                BusinessInsightCard(insight)
            }

            if (qualityDashboard.pricing.isNotEmpty()) {
                item { SectionHeader("Pricing exceptions", "Largest estimate-versus-actual differences") }
                items(qualityDashboard.pricing.take(10), key = { "pricing-${it.jobId}" }) { insight ->
                    PricingInsightCard(insight = insight, onClick = { onNavigateToJob(insight.jobId) })
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun IntelligenceBriefCard(
    briefing: String?,
    isGenerating: Boolean,
    providerLabel: String,
    configured: Boolean,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = PrimaryGreen)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Live AI briefing", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        if (configured) providerLabel else "Offline analytics available; live AI key not configured",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            briefing?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(it, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyzing operations…")
                } else {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (briefing == null) "Generate operations briefing" else "Refresh briefing")
                }
            }
        }
    }
}

@Composable
private fun OperationsMetric(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = PrimaryGreen
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(value, color = valueColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = PrimaryGreen) }
        }
    }
}

@Composable
private fun PriorityJobCard(job: PrioritizedJob, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Work, contentDescription = null, tint = riskColor(job.riskLevel))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    if (job.address.isNotBlank()) Text(job.address, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                ScoreLabel(job.score, job.riskLevel)
            }
            Text(job.reasons.joinToString(" • "), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ForecastCard(forecast: SeasonalForecast) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(forecast.species, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${forecast.confidencePercent}%", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            Text(forecast.activity, color = TextPrimary)
            Text(forecast.evidence, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Text("Action: ${forecast.recommendedAction}", color = PrimaryGreen, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PropertyRiskCard(risk: PropertyRisk) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, contentDescription = null, tint = riskColor(risk.riskLevel))
                Spacer(Modifier.width(8.dp))
                Text(risk.address, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                ScoreLabel(risk.score, risk.riskLevel)
            }
            Text("${risk.serviceCount} records • ${risk.activeJobs} active • ${risk.primaryIssue}", color = TextSecondary)
        }
    }
}

@Composable
private fun SafetyCard(signal: SafetySignal) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = riskColor(signal.severity))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${signal.title} · ${signal.affectedJobs} jobs", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(signal.action, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InventoryForecastCard(forecast: InventoryForecast) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Default.Inventory, contentDescription = null, tint = StatusPending)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(forecast.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Available: ${"%.1f".format(forecast.available)} · Suggested order: ${"%.1f".format(forecast.recommendedOrder)}", color = TextSecondary)
                Text(forecast.reason, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BusinessInsightCard(insight: OperationsInsight) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Row(modifier = Modifier.padding(14.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Text(insight.detail, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(insight.value, color = riskColor(insight.severity), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QualityCheckCard(check: QualityCheck, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(check.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (check.missing.isEmpty()) "Complete record" else "Missing: ${check.missing.joinToString()}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ScoreLabel(check.score, if (check.score >= 85) "GOOD" else if (check.score >= 60) "MODERATE" else "HIGH")
        }
    }
}

@Composable
private fun PricingInsightCard(insight: PricingInsight, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(insight.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                "Estimate $${"%.0f".format(insight.estimated)} • Actual $${"%.0f".format(insight.actual)} • Difference $${"%.0f".format(insight.variance)}",
                color = TextSecondary
            )
            Text(insight.marginSignal, color = riskColor(if (insight.variance < 0.0) "WARNING" else "GOOD"))
        }
    }
}

@Composable
private fun ScoreLabel(score: Int, label: String) {
    Column {
        Text(score.toString(), color = riskColor(label), fontWeight = FontWeight.Bold)
        Text(label, color = riskColor(label), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EmptySignal(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BackgroundCard)) {
        Text(text, color = TextSecondary, modifier = Modifier.padding(14.dp))
    }
}

private fun riskColor(label: String): Color = when (label.uppercase()) {
    "CRITICAL", "HIGH", "WARNING" -> ErrorRed
    "MODERATE", "MEDIUM" -> StatusPending
    "GOOD" -> PrimaryGreen
    else -> PrimaryGreen
}
