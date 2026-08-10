package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.BeforeAfterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeforeAfterScreen(
    jobId: String,
    onBack: () -> Unit,
    viewModel: BeforeAfterViewModel = hiltViewModel()
) {
    val photos by viewModel.photos.collectAsState()
    var beforeIdx by remember { mutableIntStateOf(0) }
    var afterIdx by remember { mutableIntStateOf(1.coerceAtMost((photos.size - 1).coerceAtLeast(0))) }
    var slider by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(jobId) { viewModel.loadForJob(jobId) }

    fun photoSource(p: com.strobingn.wildlifefieldops.data.model.Photo?): Any? {
        if (p == null) return null
        return p.remoteUrl.takeIf { it.isNotBlank() }
            ?: p.localPath.takeIf { it.isNotBlank() }
            ?: p.filePath.takeIf { it.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Before / After", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            if (photos.size < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Need at least 2 photos on this job", color = TextSecondary)
                }
            } else {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    AsyncImage(
                        model = photoSource(photos.getOrNull(afterIdx)),
                        contentDescription = "After",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    AsyncImage(
                        model = photoSource(photos.getOrNull(beforeIdx)),
                        contentDescription = "Before",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(slider)
                    )
                }

                Slider(
                    value = slider,
                    onValueChange = { slider = it },
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Before", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.Compare, null, tint = PrimaryGreen)
                    Text("After", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
