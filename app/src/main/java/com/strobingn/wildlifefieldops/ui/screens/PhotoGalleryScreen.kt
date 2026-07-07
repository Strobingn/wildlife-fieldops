package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.strobingn.wildlifefieldops.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoGalleryScreen(
    onBack: () -> Unit,
    onTakePhoto: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Inspections", "Jobs", "Before/After", "Documents")

    // Sample photo data - in production these come from the database
    val photos = remember { listOf(
        PhotoItem("1", "Entry Point", "2024-01-15", PhotoCategory.INSPECTION, "Squirrel entry in attic"),
        PhotoItem("2", "Raccoon Damage", "2024-01-14", PhotoCategory.DAMAGE, "Chewed fascia board"),
        PhotoItem("3", "Trap Set", "2024-01-13", PhotoCategory.JOB_SITE, "Live cage trap placement"),
        PhotoItem("4", "Before Repair", "2024-01-12", PhotoCategory.BEFORE, "Pre-exclusion state"),
        PhotoItem("5", "After Repair", "2024-01-12", PhotoCategory.AFTER, "Post-exclusion sealed"),
        PhotoItem("6", "Bat Guano", "2024-01-11", PhotoCategory.EVIDENCE, "Accumulation in attic"),
    ) }

    val filteredPhotos = when (selectedTab) {
        0 -> photos
        1 -> photos.filter { it.category == PhotoCategory.INSPECTION || it.category == PhotoCategory.EVIDENCE }
        2 -> photos.filter { it.category == PhotoCategory.JOB_SITE }
        3 -> photos.filter { it.category == PhotoCategory.BEFORE || it.category == PhotoCategory.AFTER }
        4 -> photos.filter { it.category == PhotoCategory.DOCUMENT }
        else -> photos
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Gallery", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onTakePhoto,
                containerColor = PrimaryGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundDark,
                contentColor = PrimaryGreen,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryGreen,
                            height = 3.dp
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = if (selectedTab == index) PrimaryGreen else TextSecondary) }
                    )
                }
            }

            if (filteredPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No photos in this category", color = TextSecondary)
                        TextButton(onClick = onTakePhoto) {
                            Text("Take a photo", color = PrimaryGreen)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPhotos, key = { it.id }) { photo ->
                        PhotoGridItem(photo = photo)
                    }
                }
            }
        }
    }
}

private enum class PhotoCategory { INSPECTION, DAMAGE, JOB_SITE, BEFORE, AFTER, EVIDENCE, DOCUMENT }

private data class PhotoItem(
    val id: String,
    val title: String,
    val date: String,
    val category: PhotoCategory,
    val description: String
)

@Composable
private fun PhotoGridItem(photo: PhotoItem) {
    val categoryColor = when (photo.category) {
        PhotoCategory.INSPECTION -> AccentBlue
        PhotoCategory.DAMAGE -> ErrorRed
        PhotoCategory.JOB_SITE -> PrimaryGreen
        PhotoCategory.BEFORE -> StatusPending
        PhotoCategory.AFTER -> SuccessGreen
        PhotoCategory.EVIDENCE -> AccentPurple
        PhotoCategory.DOCUMENT -> TextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Placeholder for photo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = TextTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }

            // Category badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(categoryColor.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    photo.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            // Info overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(photo.title, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(photo.date, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}
