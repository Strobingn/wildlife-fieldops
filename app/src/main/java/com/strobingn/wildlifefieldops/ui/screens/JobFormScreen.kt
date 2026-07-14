package com.strobingn.wildlifefieldops.ui.screens

// ... existing imports + added for tests
 import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

// In the AI button
Button(
    onClick = { viewModel.analyzePhotoWithGrok() },
    modifier = Modifier.testTag("ai_analyze_button")
) { Text("ANALYZE PHOTO & AUTO-FILL (Grok + ML Kit)") }

// Add testTag to key fields
OutlinedTextField(
    value = species,
    onValueChange = { species = it },
    label = { Text("Species") },
    modifier = Modifier.testTag("species_field").imePadding()
)

OutlinedTextField(
    value = notes,
    onValueChange = { notes = it },
    label = { Text("Notes") },
    modifier = Modifier.testTag("notes_field").imePadding()
)

// Similar for other fields and the tiered estimate button, AR button, etc.
// ... rest of the file with heavy AI wiring intact
