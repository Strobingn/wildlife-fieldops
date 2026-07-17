package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.DigitalContractDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.ContractStatus
import com.strobingn.wildlifefieldops.data.model.DigitalContract
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DigitalContractViewModel @Inject constructor(
    private val contractDao: DigitalContractDao,
    private val jobDao: JobDao
) : ViewModel() {

    val contracts = contractDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeContract(id: String): Flow<DigitalContract?> = contractDao.observeById(id)

    fun contractsForJob(jobId: String): Flow<List<DigitalContract>> = contractDao.getByJob(jobId)

    fun createForJob(
        jobId: String,
        scopeOfWork: String,
        totalAmount: Double,
        technicianName: String,
        onCreated: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val job = jobDao.getById(jobId) ?: return@launch
        val now = System.currentTimeMillis()
        val contract = DigitalContract(
            contractNumber = "CTR-$now",
            jobId = job.id,
            customerId = job.customerId,
            customerName = job.customerName,
            serviceAddress = job.address,
            scopeOfWork = scopeOfWork,
            totalAmount = totalAmount,
            technicianName = technicianName,
            status = ContractStatus.AWAITING_SIGNATURES,
            createdAt = now,
            updatedAt = now
        )
        contractDao.insert(contract)
        onCreated(contract.id)
    }

    fun saveDraft(contract: DigitalContract) = viewModelScope.launch {
        contractDao.insert(
            contract.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                syncError = null
            )
        )
    }

    fun saveTechnicianSignature(contractId: String, signerName: String, signaturePath: String) =
        viewModelScope.launch {
            val contract = contractDao.getById(contractId) ?: return@launch
            val now = System.currentTimeMillis()
            contractDao.update(
                contract.copy(
                    technicianName = signerName,
                    technicianSignaturePath = signaturePath,
                    technicianSignedAt = now,
                    status = if (contract.customerSignaturePath.isNotBlank()) {
                        ContractStatus.EXECUTED
                    } else {
                        ContractStatus.AWAITING_SIGNATURES
                    },
                    acceptedAt = if (contract.customerSignaturePath.isNotBlank()) now else null,
                    updatedAt = now,
                    isSynced = false,
                    syncError = null
                )
            )
        }

    fun saveCustomerSignature(contractId: String, signerName: String, signaturePath: String) =
        viewModelScope.launch {
            val contract = contractDao.getById(contractId) ?: return@launch
            val now = System.currentTimeMillis()
            contractDao.update(
                contract.copy(
                    customerSignerName = signerName,
                    customerSignaturePath = signaturePath,
                    customerSignedAt = now,
                    status = if (contract.technicianSignaturePath.isNotBlank()) {
                        ContractStatus.EXECUTED
                    } else {
                        ContractStatus.AWAITING_SIGNATURES
                    },
                    acceptedAt = if (contract.technicianSignaturePath.isNotBlank()) now else null,
                    updatedAt = now,
                    isSynced = false,
                    syncError = null
                )
            )
        }

    fun attachPdf(contractId: String, pdfPath: String) = viewModelScope.launch {
        val contract = contractDao.getById(contractId) ?: return@launch
        contractDao.update(
            contract.copy(
                pdfPath = pdfPath,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                syncError = null
            )
        )
    }

    fun voidContract(contractId: String) = viewModelScope.launch {
        val contract = contractDao.getById(contractId) ?: return@launch
        contractDao.update(
            contract.copy(
                status = ContractStatus.VOID,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                syncError = null
            )
        )
    }
}
