package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.ui.components.*
import com.strobingn.wildlifefieldops.ui.theme.*
import com.strobingn.wildlifefieldops.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onNavigateToJobDetail: (String) -> Unit,
    onNavigateToJobForm: () -> Unit,
    onNavigateToInspectionDetail: (String) -> Unit = {},
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allJobs by viewModel.allJobs.collectAsState()
    val allInspections by viewModel.allInspections.collectAsState()
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val dayJobs by remember(selectedDate, allJobs) {
        derivedStateOf {
            val (dayStart, dayEnd) = dayRange(selectedDate)
            allJobs.filter {
                it.scheduledDate != null && it.scheduledDate in dayStart until dayEnd &&
                it.status != JobStatus.COMPLETED && it.status != JobStatus.CANCELLED && it.status != JobStatus.PAID
            }
        }
    }

    val dayInspections by remember(selectedDate, allInspections) {
        derivedStateOf {
            val (dayStart, dayEnd) = dayRange(selectedDate)
            allInspections.filter { insp ->
                val onInspectionDay = insp.inspectionDate in dayStart until dayEnd
                val onFollowUpDay = insp.followUpRequired &&
                    insp.followUpDate != null &&
                    insp.followUpDate in dayStart until dayEnd
                onInspectionDay || onFollowUpDay
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
                inspections = allInspections,
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) }
            )

            // Day content
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (dayJobs.isEmpty() && dayInspections.isEmpty()) {
                EmptyState(
                    icon = {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = "Nothing scheduled",
                    subtitle = "Select a date with dots to see jobs or inspections",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                if (dayJobs.isNotEmpty()) {
                    Text(
                        "Jobs",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    dayJobs.forEachIndexed { index, job ->
                        FadeSlideIn(index = index) {
                            JobCard(
                                job = job,
                                onClick = { onNavigateToJobDetail(job.id) },
                            )
                        }
                    }
                }

                if (dayInspections.isNotEmpty()) {
                    Text(
                        "Inspections",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    dayInspections.forEachIndexed { index, insp ->
                        FadeSlideIn(index = dayJobs.size + index) {
                            InspectionScheduleCard(
                                inspection = insp,
                                selectedDate = selectedDate,
                                onClick = { onNavigateToInspectionDetail(insp.id) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InspectionScheduleCard(
    inspection: Inspection,
    selectedDate: Long,
    onClick: () -> Unit
) {
    val (dayStart, dayEnd) = dayRange(selectedDate)
    val isFollowUp = inspection.followUpRequired &&
        inspection.followUpDate != null &&
        inspection.followUpDate in dayStart until dayEnd &&
        inspection.inspectionDate !in dayStart until dayEnd

    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeMillis = if (isFollowUp) inspection.followUpDate!! else inspection.inspectionDate

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFollowUp) Icons.Default.EventRepeat else Icons.Default.Search,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isFollowUp) "Follow-up · ${inspection.customerName}" else inspection.customerName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(
                        inspection.inspectionType.name.lowercase().replaceFirstChar { it.uppercase() },
                        inspection.speciesIdentified.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                timeFmt.format(Date(timeMillis)),
                color = TextTertiary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun dayRange(dateMillis: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val dayStart = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return dayStart to (dayStart + 86400000L)
}

@Composable
private fun CalendarGrid(
    month: Calendar,
    jobs: List<Job>,
    inspections: List<Inspection>,
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
                        val (thisDayStart, thisDayEnd) = dayRange(thisCal.timeInMillis)

                        val hasJobs = jobs.any {
                            it.scheduledDate != null && it.scheduledDate in thisDayStart until thisDayEnd &&
                            it.status != JobStatus.COMPLETED && it.status != JobStatus.CANCELLED && it.status != JobStatus.PAID
                        }
                        val hasInspections = inspections.any { insp ->
                            insp.inspectionDate in thisDayStart until thisDayEnd ||
                            (insp.followUpRequired && insp.followUpDate != null && insp.followUpDate in thisDayStart until thisDayEnd)
                        }

                        val isSelected = selectedDate in thisDayStart until thisDayEnd
                        val isToday = today.get(Calendar.YEAR) == thisCal.get(Calendar.YEAR) &&
                                     today.get(Calendar.DAY_OF_YEAR) == thisCal.get(Calendar.DAY_OF_YEAR)

                        CalendarDayCell(
                            day = day,
                            hasJobs = hasJobs,
                            hasInspections = hasInspections,
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
    hasInspections: Boolean,
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
        if ((hasJobs || hasInspections) && !isSelected) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (hasJobs) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentBlue)
                    )
                }
                if (hasInspections) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentPurple)
                    )
                }
            }
        }
    }
}
