package com.strobingn.wildlifefieldops.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.BuildConfig
import com.strobingn.wildlifefieldops.data.local.CaptureSessionDao
import com.strobingn.wildlifefieldops.data.local.TrainingLabelDao
import com.strobingn.wildlifefieldops.data.local.VisionPredictionDao
import com.strobingn.wildlifefieldops.ml.export.ExportResult
import com.strobingn.wildlifefieldops.ml.export.TrainingLabelExporter
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MlDiagnosticsUiState(
    val trainingLabelCount: Int = 0,
    val unexportedLabelCount: Int = 0,
    val visionPredictionCount: Int = 0,
    val captureSessionCount: Int = 0,
    val committedSessionCount: Int = 0,
    val lastSessionError: String = "",
    val isExporting: Boolean = false,
    val message: String? = null,
    val pendingShareIntent: Intent? = null,
    val mlTfliteEnabled: Boolean = BuildConfig.ML_TFLITE_ENABLED,
    val mlCloudVlmEnabled: Boolean = BuildConfig.ML_CLOUD_VLM_ENABLED,
    val taxonomyAsset: String = "ml/taxonomy_v1.json"
)

@HiltViewModel
class MlDiagnosticsViewModel @Inject constructor(
    private val trainingLabelDao: TrainingLabelDao,
    private val visionPredictionDao: VisionPredictionDao,
    private val captureSessionDao: CaptureSessionDao,
    private val exporter: TrainingLabelExporter
) : ViewModel() {

    private val _ui = MutableStateFlow(MlDiagnosticsUiState())
    val ui: StateFlow<MlDiagnosticsUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val lastError = captureSessionDao.getLastWithError()?.errorMessage.orEmpty()
                _ui.update {
                    it.copy(
                        trainingLabelCount = trainingLabelDao.countAll(),
                        unexportedLabelCount = trainingLabelDao.countUnexported(),
                        visionPredictionCount = visionPredictionDao.countAll(),
                        captureSessionCount = captureSessionDao.countAll(),
                        committedSessionCount = captureSessionDao.countByStatus(CaptureSessionStatus.COMMITTED),
                        lastSessionError = lastError,
                        mlTfliteEnabled = BuildConfig.ML_TFLITE_ENABLED,
                        mlCloudVlmEnabled = BuildConfig.ML_CLOUD_VLM_ENABLED
                    )
                }
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(message = "ML diagnostics refresh failed: ${t.message ?: t.javaClass.simpleName}")
                }
            }
        }
    }

    fun exportLabels(unexportedOnly: Boolean = false) {
        viewModelScope.launch {
            _ui.update { it.copy(isExporting = true, message = null, pendingShareIntent = null) }
            when (val result = exporter.exportJsonl(unexportedOnly = unexportedOnly, markExported = true)) {
                is ExportResult.Success -> {
                    _ui.update {
                        it.copy(
                            isExporting = false,
                            message = "Exported ${result.lineCount} label(s) → ${result.file.name}",
                            pendingShareIntent = result.shareIntent
                        )
                    }
                    refresh()
                }
                is ExportResult.Empty -> {
                    _ui.update { it.copy(isExporting = false, message = result.message) }
                }
                is ExportResult.Failure -> {
                    _ui.update {
                        it.copy(isExporting = false, message = "Export failed: ${result.message}")
                    }
                }
            }
        }
    }

    fun consumeShareIntent() {
        _ui.update { it.copy(pendingShareIntent = null) }
    }

    fun clearMessage() {
        _ui.update { it.copy(message = null) }
    }
}
