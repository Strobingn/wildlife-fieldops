package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.data.model.InspectionType
import com.strobingn.wildlifefieldops.data.model.FindingSeverity
import com.strobingn.wildlifefieldops.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.strobingn.wildlifefieldops.ui.viewmodel.InspectionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionSchedulerScreen(
    onNavigateToInspectionForm: (String?) -> Unit = {},
    onNavigateToInspectionDetail: (String) -> Unit = {},
    onBack: () -> Unit,
    viewModel: InspectionsViewModel = hiltViewModel()
) {
    val inspections by viewModel.inspections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf<InspectionType?>(null) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inspection Scheduler", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Inspection", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Schedule Inspection")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Inspections",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { filterType = null }) {
                    Text("Clear Filter", color = PrimaryGreen)
                }
            }

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InspectionType.entries.forEach { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { filterType = if (filterType == type) null else type },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else {
                val filteredInspections = inspections.filter { inspection ->
                    inspection.inspectionDate >= System.currentTimeMillis() &&
                        (filterType == null || inspection.inspectionType == filterType)
                }.sortedBy { it.inspectionDate }

                if (filteredInspections.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TextSecondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No inspections scheduled",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            TextButton(onClick = { showAddDialog = true }) {
                                Text("Schedule your first inspection", color = PrimaryGreen)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredInspections) { inspection ->
                            InspectionCard(
                                inspection = inspection,
                                onClick = { onNavigateToInspectionDetail(inspection.id) },
                                dateFormat = dateFormat,
                                timeFormat = timeFormat
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInspectionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { inspection ->
                viewModel.scheduleInspection(inspection)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun InspectionCard(
    inspection: Inspection,
    onClick: () -> Unit,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inspection.customerName.ifBlank { "Unknown Customer" },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                SeverityBadge(severity = inspection.severity)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = inspection.inspectionType.name.lowercase()
                    .replaceFirstChar { it.uppercase() },
                color = PrimaryGreen,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${dateFormat.format(Date(inspection.inspectionDate))} at ${timeFormat.format(Date(inspection.inspectionDate))}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (inspection.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = inspection.notes,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (inspection.followUpRequired) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NotificationImportant,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Follow-up required",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: FindingSeverity) {
    val (color, label) = when (severity) {
        FindingSeverity.CRITICAL -> MaterialTheme.colorScheme.error to "Critical"
        FindingSeverity.HIGH -> MaterialTheme.colorScheme.tertiary to "High"
        FindingSeverity.MODERATE -> PrimaryGreen to "Medium"
        FindingSeverity.LOW -> TextSecondary to "Low"
        FindingSeverity.NONE -> TextSecondary to "None"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInspectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Inspection) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var inspectionType by remember { mutableStateOf(InspectionType.ROUTINE) }
    var severity by remember { mutableStateOf(FindingSeverity.NONE) }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Inspection", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary
                    )
                )

                // Inspection Type dropdown
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = inspectionType.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Inspection Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryGreen,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        InspectionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    inspectionType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Severity dropdown
                var severityExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = severityExpanded,
                    onExpandedChange = { severityExpanded = !severityExpanded }
                ) {
                    OutlinedTextField(
                        value = severity.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Severity") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = severityExpanded) },
                        modifier = Modifier.menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryGreen,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = severityExpanded,
                        onDismissRequest = { severityExpanded = false }
                    ) {
                        FindingSeverity.entries.forEach { sev ->
                            DropdownMenuItem(
                                text = { Text(sev.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    severity = sev
                                    severityExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date picker button
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date(selectedDate)))
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Inspection(
                            customerName = customerName,
                            inspectionType = inspectionType,
                            severity = severity,
                            inspectionDate = selectedDate,
                            notes = listOf("Address: $address".takeIf { address.isNotBlank() }, notes.trim().takeIf { it.isNotBlank() }).filterNotNull().joinToString("\n")
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = BackgroundCard
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("OK", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
