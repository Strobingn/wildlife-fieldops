package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobType
import com.strobingn.wildlifefieldops.data.pricing.PricingMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EstimateViewModel @Inject constructor(
    private val jobDao: JobDao
) : ViewModel() {

    private val _selectedJobType = MutableStateFlow(JobType.INSPECTION)
    val selectedJobType = _selectedJobType.asStateFlow()

    private val _propertySize = MutableStateFlow(PricingMatrix.PropertySize.MEDIUM)
    val propertySize = _propertySize.asStateFlow()

    private val _severity = MutableStateFlow(PricingMatrix.Severity.MODERATE)
    val severity = _severity.asStateFlow()

    private val _travelMiles = MutableStateFlow(0.0)
    val travelMiles = _travelMiles.asStateFlow()

    private val _taxRate = MutableStateFlow(8.0)
    val taxRate = _taxRate.asStateFlow()

    private val _discountPercent = MutableStateFlow(0.0)
    val discountPercent = _discountPercent.asStateFlow()

    val estimate = combine(
        selectedJobType, propertySize, severity, travelMiles, taxRate, discountPercent
    ) { params ->
        val jobType = params[0] as JobType
        val size = params[1] as PricingMatrix.PropertySize
        val sev = params[2] as PricingMatrix.Severity
        val miles = params[3] as Double
        val tax = params[4] as Double
        val discount = params[5] as Double
        val base = PricingMatrix.calculateEstimate(jobType, size, sev, miles, tax)
        val discountAmount = base.grandTotal * (discount / 100)
        base.copy(
            grandTotal = base.grandTotal - discountAmount,
            notes = base.notes + if (discount > 0) "\nDiscount applied: ${discount}%" else ""
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PricingMatrix.calculateEstimate(JobType.INSPECTION))

    val pricingInfo = selectedJobType.map { PricingMatrix.getPricing(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PricingMatrix.getPricing(JobType.INSPECTION))

    fun setJobType(type: JobType) { _selectedJobType.value = type }
    fun setPropertySize(size: PricingMatrix.PropertySize) { _propertySize.value = size }
    fun setSeverity(severity: PricingMatrix.Severity) { _severity.value = severity }
    fun setTravelMiles(miles: Double) { _travelMiles.value = miles }
    fun setTaxRate(rate: Double) { _taxRate.value = rate }
    fun setDiscount(percent: Double) { _discountPercent.value = percent }

    fun saveEstimateToJob(jobId: String, estimatedValue: Double) = viewModelScope.launch {
        val job = jobDao.getById(jobId)
        job?.let {
            jobDao.update(it.copy(estimatedValue = estimatedValue, updatedAt = System.currentTimeMillis()))
        }
    }

    fun getJobById(id: String): Flow<Job?> = flow {
        emit(jobDao.getById(id))
    }
}
