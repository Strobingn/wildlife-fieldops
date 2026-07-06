package com.strobingn.wildlifefieldops.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobType
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.JobsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateScreen(
    jobId: String,
    onBack: () -> Unit,
    jobsViewModel: JobsViewModel = hiltViewModel()
) {
    val job by jobsViewModel.getJobById(jobId).collectAsState(initial = null)

    var laborHours by remember { mutableStateOf("2.0") }
    var laborRate by remember { mutableStateOf("85.00") }
    var materialsCost by remember { mutableStateOf("0.00") }
    var equipmentCost by remember { mutableStateOf("0.00") }
    var permitCost by remember { mutableStateOf("0.00") }
    var disposalCost by remember { mutableStateOf("0.00") }
    var mileage by remember { mutableStateOf("0") }
    var mileageRate by remember { mutableStateOf("0.65") }
    var taxRate by remember { mutableStateOf("8.0") }
    var discountPercent by remember { mutableStateOf("0") }

    val laborTotal = laborHours.toDoubleOrNull()?.times(laborRate.toDoubleOrNull() ?: 0.0) ?: 0.0
    val materialsTotal = materialsCost.toDoubleOrNull() ?: 0.0
    val equipmentTotal = equipmentCost.toDoubleOrNull() ?: 0.0
    val permitTotal = permitCost.toDoubleOrNull() ?: 0.0
    val disposalTotal = disposalCost.toDoubleOrNull() ?: 0.0
    val mileageTotal = mileage.toDoubleOrNull()?.times(mileageRate.toDoubleOrNull() ?: 0.0) ?: 0.0
    val subtotal = laborTotal + materialsTotal + equipmentTotal + permitTotal + disposalTotal + mileageTotal
    val discountAmount = subtotal * (discountPercent.toDoubleOrNull() ?: 0.0) / 100.0
    val taxableAmount = subtotal - discountAmount
    val taxAmount = taxableAmount * (taxRate.toDoubleOrNull() ?: 0.0) / 100.0
    val total = taxableAmount + taxAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estimate Calculator", color = TextPrimary) },
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
            job?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(it.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(it.customerName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            // Labor
            EstimateSection(title = "Labor") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstimateField("Hours", laborHours, { laborHours = it }, Modifier.weight(1f))
                    EstimateField("Rate/hr", laborRate, { laborRate = it }, Modifier.weight(1f))
                }
                Text("Subtotal: $${String.format("%.2f", laborTotal)}", style = MaterialTheme.typography.labelSmall, color = AccentBlue, modifier = Modifier.align(Alignment.End))
            }

            // Materials & Equipment
            EstimateSection(title = "Materials & Equipment") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstimateField("Materials", materialsCost, { materialsCost = it }, Modifier.weight(1f))
                    EstimateField("Equipment", equipmentCost, { equipmentCost = it }, Modifier.weight(1f))
                }
            }

            // Other Costs
            EstimateSection(title = "Other Costs") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstimateField("Permits", permitCost, { permitCost = it }, Modifier.weight(1f))
                    EstimateField("Disposal", disposalCost, { disposalCost = it }, Modifier.weight(1f))
                }
            }

            // Mileage
            EstimateSection(title = "Mileage") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstimateField("Miles", mileage, { mileage = it }, Modifier.weight(1f))
                    EstimateField("Rate/mi", mileageRate, { mileageRate = it }, Modifier.weight(1f))
                }
            }

            // Tax and Discount
            EstimateSection(title = "Adjustments") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstimateField("Tax %", taxRate, { taxRate = it }, Modifier.weight(1f))
                    EstimateField("Discount %", discountPercent, { discountPercent = it }, Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Total Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SummaryRow("Subtotal", subtotal)
                    if (discountAmount > 0) {
                        SummaryRow("Discount (${discountPercent}%)", -discountAmount, color = SuccessGreen)
                    }
                    SummaryRow("Tax (${taxRate}%)", taxAmount)
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = BorderDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("$${String.format("%.2f", total)}", style = MaterialTheme.typography.headlineSmall, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EstimateSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EstimateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            unfocusedBorderColor = BorderDark,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = BackgroundDark,
            unfocusedContainerColor = BackgroundDark
        ),
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(10.dp),
        singleLine = true
    )
}

@Composable
private fun SummaryRow(label: String, amount: Double, color: androidx.compose.ui.graphics.Color = TextSecondary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        Text("$${String.format("%.2f", amount)}", style = MaterialTheme.typography.bodySmall, color = if (amount < 0) SuccessGreen else TextPrimary)
    }
}
