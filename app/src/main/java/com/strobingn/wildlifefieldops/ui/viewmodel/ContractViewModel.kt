package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContractViewModel @Inject constructor(
    private val jobDao: JobDao
) : ViewModel() {

    private val _job = MutableStateFlow<Job?>(null)
    val job = _job.asStateFlow()

    private val _signaturePoints = MutableStateFlow<List<Offset>>(emptyList())
    val signaturePoints = _signaturePoints.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf = _isGeneratingPdf.asStateFlow()

    private val _pdfGenerated = MutableStateFlow(false)
    val pdfGenerated = _pdfGenerated.asStateFlow()

    fun loadJob(jobId: String) = viewModelScope.launch {
        _job.value = jobDao.getById(jobId)
    }

    fun saveSignature(points: List<Offset>) {
        _signaturePoints.value = points
    }

    fun saveContract(
        customerName: String,
        address: String,
        description: String,
        estimatedValue: Double,
        warrantyMonths: Int
    ) = viewModelScope.launch {
        _job.value?.let { currentJob ->
            jobDao.update(currentJob.copy(
                customerName = customerName,
                address = address,
                description = description,
                estimatedValue = estimatedValue,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun generatePdfContract(
        customerName: String,
        address: String,
        description: String,
        estimatedValue: Double,
        warrantyMonths: Int,
        signatureDate: String
    ) = viewModelScope.launch {
        _isGeneratingPdf.value = true
        try {
            // PDF generation placeholder — requires iText or PDFBox library
            // For now, save contract metadata locally
            _job.value?.let { currentJob ->
                jobDao.update(currentJob.copy(
                    customerName = customerName,
                    address = address,
                    description = description,
                    estimatedValue = estimatedValue,
                    updatedAt = System.currentTimeMillis()
                ))
            }
            _pdfGenerated.value = true
        } catch (e: Exception) {
            android.util.Log.e("ContractViewModel", "PDF generation failed", e)
        } finally {
            _isGeneratingPdf.value = false
        }
    }

    fun clearPdfStatus() {
        _pdfGenerated.value = false
    }
}
