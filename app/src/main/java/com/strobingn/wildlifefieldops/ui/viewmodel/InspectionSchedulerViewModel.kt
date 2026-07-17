package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.InspectionDao
import com.strobingn.wildlifefieldops.data.model.FindingSeverity
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.data.model.InspectionType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InspectionSchedulerViewModel @Inject constructor(
    private val inspectionDao: InspectionDao
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val _selectedDate = MutableStateFlow(LocalDate.now(zoneId))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val scheduledInspections: StateFlow<List<Inspection>> = _selectedDate
        .flatMapLatest { date ->
            val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            inspectionDao.getScheduledBetween(start, end)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun selectDate(epochMillis: Long) {
        _selectedDate.value = Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun scheduleInspection(
        customerName: String,
        inspectorName: String,
        inspectionType: InspectionType,
        severity: FindingSeverity,
        scheduledAt: Long,
        notes: String
    ) {
        viewModelScope.launch {
            inspectionDao.insert(
                Inspection(
                    customerName = customerName.trim(),
                    inspectorName = inspectorName.trim(),
                    inspectionType = inspectionType,
                    severity = severity,
                    inspectionDate = scheduledAt,
                    notes = notes.trim(),
                    isSynced = false
                )
            )
        }
    }

    fun deleteInspection(inspection: Inspection) {
        viewModelScope.launch {
            inspectionDao.delete(inspection)
        }
    }
}
