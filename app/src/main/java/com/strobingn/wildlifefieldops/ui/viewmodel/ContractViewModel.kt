package com.strobingn.wildlifefieldops.ui.viewmodel

import android.content.Context
import android.os.Environment
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ContractViewModel @Inject constructor(
    private val jobDao: JobDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _job = MutableStateFlow<Job?>(null)
    val job = _job.asStateFlow()

    private val _signaturePoints = MutableStateFlow<List<Offset>>(emptyList())
    val signaturePoints = _signaturePoints.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf = _isGeneratingPdf.asStateFlow()

    private val _pdfGenerated = MutableStateFlow(false)
    val pdfGenerated = _pdfGenerated.asStateFlow()

    private val _pdfPath = MutableStateFlow<String?>(null)
    val pdfPath = _pdfPath.asStateFlow()

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
                warrantyMonths = warrantyMonths,
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
            withContext(Dispatchers.IO) {
                val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "contracts")
                pdfDir.mkdirs()

                val fileName = "Contract_${customerName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
                val pdfFile = File(pdfDir, fileName)

                PdfWriter(pdfFile.absolutePath).use { writer ->
                    PdfDocument(writer).use { pdfDoc ->
                        Document(pdfDoc).use { document ->
                            document.add(
                                Paragraph("WILDLIFE REMOVAL SERVICE AGREEMENT")
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(20f)
                                    .setBold()
                            )
                            document.add(Paragraph("\n"))
                            document.add(Paragraph("Date: $signatureDate").setTextAlignment(TextAlignment.RIGHT))
                            document.add(Paragraph("\n"))

                            val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f))).useAllAvailableWidth()
                            infoTable.addCell("Customer Name:")
                            infoTable.addCell(customerName)
                            infoTable.addCell("Service Address:")
                            infoTable.addCell(address)
                            document.add(infoTable)
                            document.add(Paragraph("\n"))

                            document.add(Paragraph("Description of Services").setBold().setFontSize(14f))
                            document.add(Paragraph(description))
                            document.add(Paragraph("\n"))

                            val pricingTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f))).useAllAvailableWidth()
                            pricingTable.addCell("Service")
                            pricingTable.addCell("Amount")
                            pricingTable.addCell("Estimated Cost")
                            pricingTable.addCell(String.format(Locale.US, "$%.2f", estimatedValue))
                            pricingTable.addCell("Warranty Period")
                            pricingTable.addCell("$warrantyMonths months")
                            document.add(pricingTable)
                            document.add(Paragraph("\n"))

                            document.add(Paragraph("Terms & Conditions").setBold().setFontSize(14f))
                            document.add(Paragraph("1. Wildlife Whisperer LLC agrees to perform the described wildlife removal services."))
                            document.add(Paragraph("2. Customer grants access to property for inspection, service, and follow-up visits."))
                            document.add(Paragraph("3. Payment is due upon completion of services unless otherwise agreed in writing."))
                            document.add(Paragraph("4. Warranty covers workmanship and materials for the specified period."))
                            document.add(Paragraph("5. Wildlife Whisperer LLC is not liable for pre-existing structural damage."))
                            document.add(Paragraph("6. This agreement constitutes the entire understanding between parties."))
                            document.add(Paragraph("\n"))

                            document.add(Paragraph("Electronic Signature").setBold().setFontSize(14f))
                            document.add(Paragraph("Signed by: $customerName"))
                            document.add(Paragraph("Date: $signatureDate"))
                            document.add(Paragraph("\n\n"))
                            document.add(Paragraph("_______________________________").setTextAlignment(TextAlignment.LEFT))
                            document.add(Paragraph("Customer Signature"))
                        }
                    }
                }

                _pdfPath.value = pdfFile.absolutePath
                _pdfGenerated.value = true

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

        } catch (e: Exception) {
            android.util.Log.e("ContractViewModel", "PDF generation failed", e)
            _pdfPath.value = null
            _pdfGenerated.value = false
        } finally {
            _isGeneratingPdf.value = false
        }
    }

    fun clearPdfStatus() {
        _pdfGenerated.value = false
        _pdfPath.value = null
    }
}
