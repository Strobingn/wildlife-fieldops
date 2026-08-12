package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.PredictiveViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.SuggestedSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictiveScreen(
    onBack: () -> Unit,
    viewModel: PredictiveViewModel = hiltViewModel()
) {
    val suggestions by viewModel.suggestions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Predictive Schedule", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = PrimaryGreen, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Smart next actions",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Priority + tide + value scoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (suggestions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No open jobs to score. Add jobs to see predictive suggestions.",
                            color = TextSecondary,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(suggestions) { slot ->
                    SuggestionCard(slot)
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(slot: SuggestedSlot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    slot.job.title.ifBlank { slot.job.customerName.ifBlank { "Job" } },
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${slot.score.toInt()}",
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (slot.job.address.isNotBlank()) {
                Text(
                    slot.job.address,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                slot.reason,
                color = TextTertiary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Schedule, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Suggested ~${slot.suggestedHour}:00",
                    color = AccentBlue,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
