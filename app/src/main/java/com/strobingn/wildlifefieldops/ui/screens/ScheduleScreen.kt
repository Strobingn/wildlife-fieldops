package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onNavigateToJobDetail: (String) -> Unit,
    onNavigateToJobForm: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allJobs by viewModel.allJobs.collectAsState()
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val dayJobs by remember(selectedDate, allJobs) {
        derivedStateOf {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val dayStart = cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val dayEnd = dayStart + 86400000L
            allJobs.filter {
                it.scheduledDate != null && it.scheduledDate in dayStart until dayEnd &&
                it.status != JobStatus.COMPLETED && it.status != JobStatus.CANCELLED && it.status != JobStatus.PAID
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule", color = TextPrimary) },
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
                onClick = onNavigateToJobForm,
                containerColor = PrimaryGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Job")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Month Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentMonth = Calendar.getInstance().apply {
                        timeInMillis = currentMonth.timeInMillis
                        add(Calendar.MONTH, -1)
                    }
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = TextPrimary)
                }
                Text(
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    currentMonth = Calendar.getInstance().apply {
                        timeInMillis = currentMonth.timeInMillis
                        add(Calendar.MONTH, 1)
                    }
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = TextPrimary)
                }
            }

            // Calendar Grid
            CalendarGrid(
                month = currentMonth,
                jobs = allJobs,
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) }
            )

            // Day's Jobs
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Jobs for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (dayJobs.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "No jobs scheduled",
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                dayJobs.forEach { job ->
                    JobCard(
                        job = job,
                        onClick = { onNavigateToJobDetail(job.id) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CalendarGrid(
    month: Calendar,
    jobs: List<Job>,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()

    val dayOfWeekNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            dayOfWeekNames.forEach { dayName ->
                Text(
                    dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar days
        var day = 1
        for (week in 0..5) {
            if (day > daysInMonth) break
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (dow in 0..6) {
                    if ((week == 0 && dow < firstDayOfWeek) || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val thisCal = Calendar.getInstance().apply {
                            timeInMillis = cal.timeInMillis
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        val thisDayStart = thisCal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                        val thisDayEnd = thisDayStart + 86400000L
                        val hasJobs = jobs.any {
                            it.scheduledDate != null && it.scheduledDate in thisDayStart until thisDayEnd &&
                            it.status != JobStatus.COMPLETED && it.status != JobStatus.CANCELLED && it.status != JobStatus.PAID
                        }
                        val isSelected = selectedDate in thisDayStart until thisDayEnd
                        val isToday = today.get(Calendar.YEAR) == thisCal.get(Calendar.YEAR) &&
                                     today.get(Calendar.DAY_OF_YEAR) == thisCal.get(Calendar.DAY_OF_YEAR)

                        CalendarDayCell(
                            day = day,
                            hasJobs = hasJobs,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateSelected(thisDayStart) },
                            modifier = Modifier.weight(1f)
                        )
                        day++
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    hasJobs: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> PrimaryGreen
                    isToday -> PrimaryGreen.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            day.toString(),
            color = when {
                isSelected -> Color.Black
                isToday -> PrimaryGreen
                else -> TextPrimary
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (hasJobs && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentBlue)
            )
        }
    }
}
