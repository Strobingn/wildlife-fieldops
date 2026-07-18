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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.FindingSeverity
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.ui.viewmodel.InspectionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionListScreen(
    onNavigateToInspectionDetail: (String) -> Unit,
    onNavigateToInspectionForm: () -> Unit,
    onBack: () -> Unit,
    viewModel: InspectionsViewModel = hiltViewModel()
) {
    val inspections by viewModel.inspections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val followUps = inspections.count { it.followUpRequired }
    val highPriority = inspections.count { it.severity == FindingSeverity.HIGH || it.severity == FindingSeverity.CRITICAL }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inspections", fontWeight = FontWeight.Bold)
                        Text("Property findings and field reports", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onNavigateToInspectionForm) { Icon(Icons.Default.Add, "New inspection") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToInspectionForm,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New inspection") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Total", inspections.size.toString(), Icons.Default.FactCheck, Modifier.weight(1f))
                SummaryCard("Follow-ups", followUps.toString(), Icons.Default.AssignmentLate, Modifier.weight(1f))
                SummaryCard("High risk", highPriority.toString(), Icons.Default.WarningAmber, Modifier.weight(1f))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search customer, species or inspector") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Close, "Clear") }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (inspections.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            Text("No inspections found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Create a professional property inspection report.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = onNavigateToInspectionForm) { Text("Create inspection") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(inspections, key = { it.id }) { inspection ->
                        InspectionCard(inspection) { onNavigateToInspectionDetail(inspection.id) }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InspectionCard(inspection: Inspection, onClick: () -> Unit) {
    val severityColor = when (inspection.severity) {
        FindingSeverity.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        FindingSeverity.LOW -> Color(0xFF7F8B82)
        FindingSeverity.MODERATE -> Color(0xFFD09B48)
        FindingSeverity.HIGH -> Color(0xFFD06A5F)
        FindingSeverity.CRITICAL -> Color(0xFFBD4141)
    }
    val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(inspection.inspectionDate))

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(Icons.Default.HomeWork, null, Modifier.padding(11.dp).size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(inspection.customerName.ifBlank { "Unknown customer" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(inspection.inspectionType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } + " inspection", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.clip(RoundedCornerShape(50)).background(severityColor.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(inspection.severity.name, color = severityColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                InspectionDetail(Icons.Default.CalendarToday, date, Modifier.weight(1f))
                InspectionDetail(Icons.Default.Person, inspection.inspectorName.ifBlank { "Unassigned" }, Modifier.weight(1f))
            }

            if (inspection.speciesIdentified.isNotBlank()) {
                InspectionDetail(Icons.Default.Pets, inspection.speciesIdentified, Modifier.fillMaxWidth())
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (inspection.followUpRequired) {
                    AssistChip(onClick = onClick, label = { Text("Follow-up required") }, leadingIcon = { Icon(Icons.Default.AssignmentLate, null, Modifier.size(16.dp)) })
                } else {
                    Text("Report current", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InspectionDetail(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(7.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
