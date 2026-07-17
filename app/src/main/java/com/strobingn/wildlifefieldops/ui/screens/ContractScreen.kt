package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.ui.components.SignaturePadWithControls
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.ContractViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractScreen(
    jobId: String? = null,
    onBack: () -> Unit,
    viewModel: ContractViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    var customerName by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var serviceDescription by remember { mutableStateOf("") }
    var estimatedValue by remember { mutableStateOf("") }
    var warrantyMonths by remember { mutableStateOf("12") }
    var showSignaturePad by remember { mutableStateOf(false) }
    var signatureCaptured by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    val today = dateFormat.format(Date())

    LaunchedEffect(jobId) {
        jobId?.let { viewModel.loadJob(it) }
    }

    val job by viewModel.job.collectAsState()

    LaunchedEffect(job) {
        job?.let {
            customerName = it.customerName
            customerAddress = it.address
            serviceDescription = it.description
            estimatedValue = it.estimatedValue.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Contract", color = TextPrimary) },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contract header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "WILDLIFE REMOVAL SERVICE AGREEMENT",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Date: $today", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Customer info
            ContractSection("Customer Information") {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = { customerAddress = it },
                    label = { Text("Service Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary
                    )
                )
            }

            // Service details
            ContractSection("Service Details") {
                OutlinedTextField(
                    value = serviceDescription,
                    onValueChange = { serviceDescription = it },
                    label = { Text("Description of Services") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = estimatedValue,
                        onValueChange = { estimatedValue = it },
                        label = { Text("Estimated Cost ($)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryGreen,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = warrantyMonths,
                        onValueChange = { warrantyMonths = it },
                        label = { Text("Warranty (mo)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryGreen,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                }
            }

            // Terms
            ContractSection("Terms & Conditions") {
                val terms = buildString {
                    append("1. Wildlife Whisperer LLC agrees to perform the described wildlife removal services.\n\n")
                    append("2. Customer grants access to property for inspection, service, and follow-up visits.\n\n")
                    append("3. Payment is due upon completion of services unless otherwise agreed in writing.\n\n")
                    append("4. Warranty covers workmanship and materials for the specified period.\n\n")
                    append("5. Wildlife Whisperer LLC is not liable for pre-existing structural damage.\n\n")
                    append("6. This agreement constitutes the entire understanding between parties.")
                }
                Text(terms, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            // Signature section
            ContractSection("Electronic Signature") {
                if (!signatureCaptured) {
                    if (!showSignaturePad) {
                        Button(
                            onClick = { showSignaturePad = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Contract")
                        }
                    } else {
                        SignaturePadWithControls(
                            onSignatureCaptured = { points ->
                                viewModel.saveSignature(points)
                                signatureCaptured = true
                                showSignaturePad = false
                            },
                            onClear = { signatureCaptured = false }
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Signature captured", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Signed by: $customerName", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("Date: $today", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            signatureCaptured = false
                            showSignaturePad = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Re-sign", color = TextSecondary)
                    }
                }
            }

            // Action buttons
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showPdfDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = signatureCaptured && customerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate PDF Contract")
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    viewModel.saveContract(
                        customerName = customerName,
                        address = customerAddress,
                        description = serviceDescription,
                        estimatedValue = estimatedValue.toDoubleOrNull() ?: 0.0,
                        warrantyMonths = warrantyMonths.toIntOrNull() ?: 12
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = customerName.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Contract")
            }
        }
    }

    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            title = { Text("Generate PDF", color = TextPrimary) },
            text = { Text("This will generate a PDF contract and save it locally. You can upload it to Supabase later.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.generatePdfContract(
                            customerName = customerName,
                            address = customerAddress,
                            description = serviceDescription,
                            estimatedValue = estimatedValue.toDoubleOrNull() ?: 0.0,
                            warrantyMonths = warrantyMonths.toIntOrNull() ?: 12,
                            signatureDate = today
                        )
                        showPdfDialog = false
                    }
                ) {
                    Text("Generate", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BackgroundCard
        )
    }
}

@Composable
private fun ContractSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}
