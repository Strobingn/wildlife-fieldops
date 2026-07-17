package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.DefaultServiceTypes
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.ui.theme.BackgroundDark
import com.strobingn.wildlifefieldops.ui.theme.PrimaryGreen
import com.strobingn.wildlifefieldops.ui.theme.TextPrimary
import com.strobingn.wildlifefieldops.ui.viewmodel.JobsViewModel
import kotlinx.coroutines.launch

private data class ImportedJobDraft(
    val title: String,
    val customerName: String,
    val address: String,
    val description: String,
    val notes: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sourceText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Import job", color = TextPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Paste an SMS or email. Wildlife FieldOps extracts the customer, address, subject, and job details locally on the device.",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = sourceText,
                onValueChange = { sourceText = it },
                label = { Text("SMS or email text") },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                minLines = 6
            )

            Button(
                onClick = {
                    val draft = parseImportedJob(sourceText)
                    title = draft.title
                    customerName = draft.customerName
                    address = draft.address
                    description = draft.description
                    notes = draft.notes
                },
                enabled = sourceText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Extract job details")
            }

            OutlinedTextField(title, { title = it }, label = { Text("Job title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(customerName, { customerName = it }, label = { Text("Customer name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text("Service address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                description,
                { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                minLines = 4
            )
            OutlinedTextField(
                notes,
                { notes = it },
                label = { Text("Import notes") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                minLines = 3
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isBlank() || isSaving) return@Button
                        isSaving = true
                        scope.launch {
                            runCatching {
                                viewModel.createJobNow(
                                    title = title.trim(),
                                    description = description.trim(),
                                    customerId = "",
                                    customerName = customerName.trim(),
                                    address = address.trim(),
                                    type = DefaultServiceTypes.all.first(),
                                    priority = JobPriority.MEDIUM,
                                    estimatedValue = 0.0,
                                    scheduledDate = null,
                                    notes = notes.trim()
                                )
                            }.onSuccess {
                                snackbarHostState.showSnackbar("Imported job created")
                                onBack()
                            }.onFailure {
                                snackbarHostState.showSnackbar("Import failed: ${it.message ?: "unknown error"}")
                            }
                            isSaving = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank() && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Create job")
                }
            }
        }
    }
}

private fun parseImportedJob(raw: String): ImportedJobDraft {
    val normalized = raw.replace("\r\n", "\n").trim()
    val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

    fun valueFor(vararg labels: String): String? {
        val labelPattern = labels.joinToString("|") { Regex.escape(it) }
        val regex = Regex("^(?:$labelPattern)\\s*[:=-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        return lines.firstNotNullOfOrNull { regex.find(it)?.groupValues?.getOrNull(1)?.trim() }
    }

    val emailFrom = Regex("^from:\\s*(.+)$", RegexOption.IGNORE_CASE)
        .find(lines.firstOrNull { it.startsWith("from:", true) }.orEmpty())
        ?.groupValues?.getOrNull(1)
        ?.substringBefore("<")
        ?.trim()

    val customer = valueFor("customer", "name", "contact") ?: emailFrom.orEmpty()
    val subject = valueFor("subject", "service", "request", "job")
    val address = valueFor("address", "location", "service address")
        ?: lines.firstOrNull { looksLikeAddress(it) }
        .orEmpty()

    val bodyLines = lines.filterNot { line ->
        line.startsWith("from:", true) ||
            line.startsWith("to:", true) ||
            line.startsWith("date:", true) ||
            line.startsWith("subject:", true) ||
            line.equals(address, true)
    }
    val description = bodyLines.joinToString("\n").take(2_000)
    val inferredTitle = subject?.takeIf { it.isNotBlank() }
        ?: bodyLines.firstOrNull()?.take(80)
        ?: "Imported wildlife service request"

    return ImportedJobDraft(
        title = inferredTitle,
        customerName = customer,
        address = address,
        description = description,
        notes = "Imported from pasted SMS/email text. Original source retained below:\n\n${normalized.take(4_000)}"
    )
}

private fun looksLikeAddress(value: String): Boolean {
    val streetWords = "street|st\\.?|avenue|ave\\.?|road|rd\\.?|drive|dr\\.?|lane|ln\\.?|court|ct\\.?|boulevard|blvd\\.?|highway|hwy\\.?|route"
    return Regex("^\\d{1,6}\\s+.+\\b($streetWords)\\b", RegexOption.IGNORE_CASE).containsMatchIn(value)
}
