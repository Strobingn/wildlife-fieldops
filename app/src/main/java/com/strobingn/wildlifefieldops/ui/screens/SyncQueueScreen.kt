package com.strobingn.wildlifefieldops.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.PendingOperation
import com.strobingn.wildlifefieldops.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.strobingn.wildlifefieldops.ui.viewmodel.SyncQueueViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncQueueScreen(
    onBack: () -> Unit,
    viewModel: SyncQueueViewModel = hiltViewModel()
) {
    val operations by viewModel.operations.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sync Queue", color = TextPrimary)
                        if (pendingCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                Text(pendingCount.toString(), color = Color.White)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (operations.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                        }
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
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else if (operations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = PrimaryGreen.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "All synced up",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "No pending operations",
                            color = TextSecondary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(operations) { operation ->
                        OperationCard(
                            operation = operation,
                            dateFormat = dateFormat,
                            onRetry = { viewModel.retryOperation(operation) },
                            onDelete = { viewModel.deleteOperation(operation) }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Operations?", color = TextPrimary) },
            text = { Text("This will remove ${operations.size} pending operations. They will not be synced.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BackgroundCard
        )
    }
}

@Composable
private fun OperationCard(
    operation: PendingOperation,
    dateFormat: SimpleDateFormat,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusColor, statusIcon) = when {
        operation.isProcessing -> PrimaryGreen to Icons.Default.Sync
        operation.retryCount >= 5 -> MaterialTheme.colorScheme.error to Icons.Default.Error
        operation.retryCount > 0 -> MaterialTheme.colorScheme.tertiary to Icons.Default.Warning
        else -> TextSecondary to Icons.Default.Schedule
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${operation.operationType.name} ${operation.entityType.name}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = dateFormat.format(Date(operation.createdAt)),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Entity ID: ${operation.entityId}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            if (operation.retryCount > 0) {
                Text(
                    text = "Retries: ${operation.retryCount}/5",
                    color = if (operation.retryCount >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (operation.lastError.isNotBlank()) {
                Text(
                    text = "Error: ${operation.lastError}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (operation.retryCount >= 5) {
                    TextButton(onClick = onRetry) {
                        Text("Retry", color = PrimaryGreen)
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
