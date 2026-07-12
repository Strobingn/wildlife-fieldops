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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFormScreen(
    jobId: String? = null,
    onBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
    serviceTypesViewModel: ServiceTypesViewModel = hiltViewModel()
) {
    val serviceTypes by serviceTypesViewModel.allTypes.collectAsState()
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

    LaunchedEffect(jobId) {
        jobId?.let { id ->
            viewModel.getJobById(id).collect { job ->
                job?.let {
                    isEditing = true
                    title = it.title
                    description = it.description
                    customerId = it.customerId
                    customerName = it.customerName
                    address = it.address
                    selectedType = DefaultServiceTypes.display(it.type)
                    selectedPriority = it.priority
                    estimatedValue = if (it.estimatedValue > 0) it.estimatedValue.toString() else ""
                    notes = it.notes
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Job" else "New Job", color = TextPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    val estVal = estimatedValue.toDoubleOrNull() ?: 0.0
                    val service = DefaultServiceTypes.display(selectedType)
                    if (isEditing && jobId != null) {
                        val updatedJob = Job(
                            id = jobId,
                            title = title,
                            description = description,
                            customerId = customerId,
                            customerName = customerName,
                            address = address,
                            type = service,
                            priority = selectedPriority,
                            estimatedValue = estVal,
                            notes = notes,
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.updateJob(updatedJob)
                    } else {
                        viewModel.createJob(
                            title = title,
                            description = description,
                            customerId = customerId,
                            customerName = customerName,
                            address = address,
                            type = service,
                            priority = selectedPriority,
                            estimatedValue = estVal,
                            scheduledDate = null,
                            notes = notes
                        )
                    }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditing) "Update Job" else "Create Job", fontWeight = FontWeight.Bold)
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
