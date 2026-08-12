package com.strobingn.wildlifefieldops.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ai.HybridAIService
import com.strobingn.wildlifefieldops.data.model.*
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.JobsViewModel
import com.strobingn.wildlifefieldops.ui.viewmodel.ServiceTypesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFormScreen(
    jobId: String? = null,
    onBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
    serviceTypesViewModel: ServiceTypesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
    var actualCost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var scheduledDateMillis by remember { mutableStateOf<Long?>(null) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(jobId == null) }
    var existingJob by remember { mutableStateOf<Job?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var aiStatus by remember { mutableStateOf("") }

    // Date / Time picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    val dateFormatter = remember {
        SimpleDateFormat("EEE, MMM d, yyyy  h:mm a", Locale.getDefault())
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedPhoto = uri
        scope.launch {
            isAnalyzing = true
            aiStatus = "Analyzing photo…"
            runCatching {
                HybridAIService.analyzePhotoAndFillForm(
                    context = context,
                    imageUri = uri,
                    jobContext = listOf(address, description, notes).filter { it.isNotBlank() }.joinToString(" | ")
                )
            }.onSuccess { result ->
                species = result.species.joinToString(", ")
                if (result.serviceType.isNotBlank()) selectedType = DefaultServiceTypes.display(result.serviceType)
                selectedPriority = runCatching { JobPriority.valueOf(result.priority.uppercase()) }
                    .getOrDefault(selectedPriority)
                if (result.notes.isNotBlank()) {
                    notes = listOf(notes.trim(), "AI photo analysis: ${result.notes.trim()}")
                        .filter { it.isNotBlank() }.joinToString("\n\n")
                }
                val mid = (result.estimatedPriceLow + result.estimatedPriceHigh) / 2.0
                if (estimatedValue.isBlank() && mid > 0) estimatedValue = String.format("%.2f", mid)
                if (title.isBlank() && species.isNotBlank()) title = "$species removal"
                aiStatus = if (result.fromGrok) "Live Grok analysis applied" else "Offline ML analysis applied"
            }.onFailure {
                aiStatus = "Photo analysis failed: ${it.message ?: "unknown error"}"
            }
            isAnalyzing = false
        }
    }

    LaunchedEffect(jobId) {
        if (jobId.isNullOrBlank()) {
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
            actualCost = if (job.actualCost > 0) job.actualCost.toString() else ""
            notes = job.notes
            scheduledDateMillis = job.scheduledDate
        }
        loaded = true
    }

    // DatePicker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = scheduledDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        pendingDateMillis = selected
                        showDatePicker = false
                        showTimePicker = true
                    } else {
                        showDatePicker = false
                    }
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker dialog
    if (showTimePicker) {
        val initialCal = Calendar.getInstance().apply {
            timeInMillis = pendingDateMillis ?: scheduledDateMillis ?: System.currentTimeMillis()
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = {
                Box(Modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val base = pendingDateMillis ?: scheduledDateMillis ?: System.currentTimeMillis()
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = base
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    scheduledDateMillis = cal.timeInMillis
                    pendingDateMillis = null
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Allow clearing the date
                    scheduledDateMillis = null
                    pendingDateMillis = null
                    showTimePicker = false
                }) { Text("Clear") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            return@Scaffold
        }
        if (jobId != null && existingJob == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Job not found", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Go back") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccentPurple.copy(alpha = 0.14f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI photo autofill", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Select a wildlife or damage photo. The app identifies likely species, service type, priority, notes, and a starting estimate.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { photoPicker.launch("image/*") },
                        enabled = !isAnalyzing,
                        modifier = Modifier.fillMaxWidth().testTag("ai_analyze_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        if (isAnalyzing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isAnalyzing) "Analyzing…" else "Analyze photo and fill form")
                    }
                    if (selectedPhoto != null) Text("Photo selected", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    if (aiStatus.isNotBlank()) Text(aiStatus, color = if (aiStatus.contains("failed", true)) ErrorRed else PrimaryGreen, style = MaterialTheme.typography.bodySmall)
                }
            }

            FormField(title, { title = it }, "Job Title *", Icons.Default.Work)
            FormField(customerName, { customerName = it }, "Customer Name", Icons.Default.Person)
            FormField(address, { address = it }, "Address", Icons.Default.LocationOn, KeyboardCapitalization.Words)
            FormField(species, { species = it }, "Species / evidence", Icons.Default.Pets, modifier = Modifier.testTag("species_field"))

            // Scheduled Date & Time picker field
            OutlinedTextField(
                value = scheduledDateMillis?.let { dateFormatter.format(Date(it)) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Scheduled Date & Time") },
                placeholder = { Text("Tap to select date & time") },
                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (scheduledDateMillis != null) {
                        IconButton(onClick = { scheduledDateMillis = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear date", tint = TextSecondary)
                        }
                    } else {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextSecondary)
                    }
                },
                colors = jobFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .testTag("scheduled_date_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(showTypeDropdown, { showTypeDropdown = it }, Modifier.weight(1f)) {
                    OutlinedTextField(selectedType, {}, readOnly = true, label = { Text("Service type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showTypeDropdown) },
                        colors = jobFieldColors(), modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(showTypeDropdown, { showTypeDropdown = false }) {
                        serviceTypes.forEach { type -> DropdownMenuItem({ Text(type) }, { selectedType = type; showTypeDropdown = false }) }
                        DropdownMenuItem({ Text("+ Add service", color = PrimaryGreen) }, { showTypeDropdown = false; showAddServiceDialog = true })
                    }
                }
                ExposedDropdownMenuBox(showPriorityDropdown, { showPriorityDropdown = it }, Modifier.weight(1f)) {
                    OutlinedTextField(selectedPriority.name, {}, readOnly = true, label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showPriorityDropdown) },
                        colors = jobFieldColors(), modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(showPriorityDropdown, { showPriorityDropdown = false }) {
                        JobPriority.entries.forEach { p -> DropdownMenuItem({ Text(p.name) }, { selectedPriority = p; showPriorityDropdown = false }) }
                    }
                }
            }

            OutlinedTextField(
                estimatedValue,
                { estimatedValue = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Estimated Value") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                colors = jobFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                actualCost,
                { actualCost = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Actual Cost") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                colors = jobFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("actual_cost_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, colors = jobFieldColors(),
                modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 4, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, colors = jobFieldColors(),
                modifier = Modifier.fillMaxWidth().height(120.dp).testTag("notes_field"), maxLines = 6, shape = RoundedCornerShape(12.dp))

            Button(
                onClick = {
                    if (title.isBlank() || isSaving) return@Button
                    isSaving = true
                    val value = estimatedValue.toDoubleOrNull() ?: existingJob?.estimatedValue ?: 0.0
                    val actualCostValue = actualCost.toDoubleOrNull() ?: existingJob?.actualCost ?: 0.0
                    val mergedDescription = if (species.isBlank() || description.contains(species, true)) description.trim()
                        else listOf("Species/evidence: $species", description.trim()).filter { it.isNotBlank() }.joinToString("\n")
                    if (isEditing && jobId != null) {
                        viewModel.updateJobDetails(
                            jobId,
                            title.trim(),
                            mergedDescription,
                            customerId,
                            customerName.trim(),
                            address.trim(),
                            DefaultServiceTypes.display(selectedType),
                            selectedPriority,
                            value,
                            actualCostValue,
                            notes.trim(),
                            scheduledDateMillis
                        )
                    } else {
                        viewModel.createJob(
                            title.trim(),
                            mergedDescription,
                            customerId,
                            customerName.trim(),
                            address.trim(),
                            DefaultServiceTypes.display(selectedType),
                            selectedPriority,
                            value,
                            scheduledDateMillis,
                            notes.trim()
                        )
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar(if (isEditing) "Job updated" else "Job created")
                        isSaving = false
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSaving) "Saving…" else if (isEditing) "Save changes" else "Create Job", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showAddServiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddServiceDialog = false },
            title = { Text("New service type") },
            text = { OutlinedTextField(newServiceName, { newServiceName = it }, label = { Text("Service name") }) },
            confirmButton = {
                TextButton(onClick = {
                    val name = DefaultServiceTypes.normalize(newServiceName)
                    if (name.isNotBlank()) { serviceTypesViewModel.addType(name); selectedType = name }
                    newServiceName = ""; showAddServiceDialog = false
                }, enabled = newServiceName.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddServiceDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        colors = jobFieldColors(), modifier = modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = capitalization), shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun jobFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = BorderDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextTertiary,
    focusedContainerColor = BackgroundCard,
    unfocusedContainerColor = BackgroundCard
)
