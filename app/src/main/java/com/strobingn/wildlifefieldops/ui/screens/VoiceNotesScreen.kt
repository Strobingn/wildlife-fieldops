package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.VoiceNote
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.VoiceNotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNotesScreen(
    onBack: () -> Unit,
    viewModel: VoiceNotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Notes", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.addDemoNote() }) {
                        Icon(Icons.Default.Mic, "Add demo note", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addDemoNote() },
                containerColor = PrimaryGreen,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Default.Mic, "Record")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Mic, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No voice notes yet", color = TextSecondary)
                    Text("Tap the mic to add a field note", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    VoiceNoteCard(
                        note = note,
                        onDelete = { viewModel.delete(note) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceNoteCard(note: VoiceNote, onDelete: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        note.title.ifBlank { "Voice note" },
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
            if (note.transcript.isNotBlank()) {
                Text(
                    note.transcript,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${note.durationMs / 1000}s",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    dateFmt.format(Date(note.createdAt)),
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
