package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.*
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.JobsViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.ServiceTypesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFormScreen(
    jobId: String? = null,
    onBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
    serviceTypesViewModel: ServiceTypesViewModel = hiltViewModel()
) {
    val serviceTypes by serviceTypesViewModel.allTypes.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DefaultServiceTypes.all.first()) }
    var selectedPriority by remember { mutableStateOf(JobPriority.MEDIUM) }
    var estimatedValue by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(jobId == null) }
    var existingJob by remember { mutableStateOf<Job?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Load job once when editing so party-entered jobs can be revised later.
    LaunchedEffect(jobId) {
        if (jobId.isNullOrBlank()) {
            isEditing = false
            loaded = true
            return@LaunchedEffect
        }
        val job = viewModel.loadJobOnce(jobId)
        if (job != null) {
            isEditing = true
            existingJob = job
            title = job.title
            description = job.description
            customerId = job.customerId
            customerName = job.customerName
            address = job.address
            selectedType = DefaultServiceTypes.display(job.type)
            selectedPriority = job.priority
            estimatedValue = if (job.estimatedValue > 0) job.estimatedValue.toString() else ""
            notes = job.notes
        }
        loaded = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Job" else "New Job",
                        color = TextPrimary
                    )
                },
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
        if (!loaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            return@Scaffold
        }

        if (jobId != null && existingJob == null && loaded) {
            // jobId provided but not found
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                Text("Job not found", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This job may have been deleted. You can create a new one instead.",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                    Text("Go back", color = Color.Black)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                Text(
                    "Update any field, then tap Save. Status, photos, and invoices are kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Job Title *") },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = TextSecondary) },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary) },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Service type + Priority
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = showTypeDropdown,
                    onExpandedChange = { showTypeDropdown = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                        colors = fieldColors(),
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        serviceTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = TextPrimary) },
                                onClick = {
                                    selectedType = type
                                    showTypeDropdown = false
                                }
                            )
                        }
                        HorizontalDivider(color = BorderDark)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "+ Add new service type",
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                showTypeDropdown = false
                                showAddServiceDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryGreen)
                            }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showPriorityDropdown,
                    onExpandedChange = { showPriorityDropdown = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedPriority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPriorityDropdown) },
                        colors = fieldColors(),
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showPriorityDropdown,
                        onDismissRequest = { showPriorityDropdown = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        JobPriority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.name, color = TextPrimary) },
                                onClick = {
                                    selectedPriority = priority
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = estimatedValue,
                onValueChange = { estimatedValue = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Estimated Value") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = TextSecondary) },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (title.isBlank() || isSaving) return@Button
                    isSaving = true
                    val estVal = estimatedValue.toDoubleOrNull() ?: 0.0
                    val service = DefaultServiceTypes.display(selectedType)
                    if (isEditing && jobId != null) {
                        viewModel.updateJobDetails(
                            jobId = jobId,
                            title = title.trim(),
                            description = description.trim(),
                            customerId = customerId,
                            customerName = customerName.trim(),
                            address = address.trim(),
                            type = service,
                            priority = selectedPriority,
                            estimatedValue = estVal,
                            notes = notes.trim()
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Job updated")
                            isSaving = false
                            onBack()
                        }
                    } else {
                        viewModel.createJob(
                            title = title.trim(),
                            description = description.trim(),
                            customerId = customerId,
                            customerName = customerName.trim(),
                            address = address.trim(),
                            type = service,
                            priority = selectedPriority,
                            estimatedValue = estVal,
                            scheduledDate = null,
                            notes = notes.trim()
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Job created")
                            isSaving = false
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    when {
                        isSaving -> "Saving…"
                        isEditing -> "Save changes"
                        else -> "Create Job"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (isEditing) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddServiceDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddServiceDialog = false
                newServiceName = ""
            },
            title = { Text("New service type", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Add a service your crew performs (e.g. “Gutter guard install”).",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newServiceName,
                        onValueChange = { newServiceName = it },
                        label = { Text("Service name") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = DefaultServiceTypes.normalize(newServiceName)
                        if (name.isNotBlank()) {
                            serviceTypesViewModel.addType(name)
                            selectedType = name
                            showAddServiceDialog = false
                            newServiceName = ""
                        }
                    },
                    enabled = newServiceName.isNotBlank()
                ) {
                    Text("Add", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddServiceDialog = false
                    newServiceName = ""
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BackgroundCard
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = BorderDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextTertiary,
    focusedContainerColor = BackgroundCard,
    unfocusedContainerColor = BackgroundCard
)
