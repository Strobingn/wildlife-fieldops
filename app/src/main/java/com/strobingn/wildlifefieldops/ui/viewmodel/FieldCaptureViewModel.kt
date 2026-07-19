package com.strobingn.wildlifefieldops.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.strobingn.wildlifefieldops.data.local.PhotoDao
import com.strobingn.wildlifefieldops.data.model.Photo
import com.strobingn.wildlifefieldops.data.model.PhotoCategory
import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.data.repository.CaptureSessionRepository
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitRequest
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitResult
import com.strobingn.wildlifefieldops.ml.domain.MultimodalFusionEngine
import com.strobingn.wildlifefieldops.ml.domain.VisionAnalyzer
import com.strobingn.wildlifefieldops.ml.domain.VoiceJobParser
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import com.strobingn.wildlifefieldops.ml.model.DraftHints
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import com.strobingn.wildlifefieldops.ml.model.GpsFix
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class FieldCapturePhase {
    CAPTURE,
    ANALYZING,
    REVIEW,
    SAVING
}

data class FieldCapturePhotoUi(
    val photoId: String,
    val localPath: String,
    val displayName: String
)

data class FieldCaptureUiState(
    val phase: FieldCapturePhase = FieldCapturePhase.CAPTURE,
    val sessionId: String = "",
    val transcript: String = "",
    val photos: List<FieldCapturePhotoUi> = emptyList(),
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val gpsAccuracy: Float? = null,
    val addressGuess: String = "",
    val draft: MultimodalDraftSnapshot = MultimodalDraftSnapshot(),
    val warnings: List<String> = emptyList(),
    val error: String? = null,
    val infoMessage: String? = null,
    val isRefreshingGps: Boolean = false,
    val committedJobId: String? = null,
    val ready: Boolean = false
)

@HiltViewModel
class FieldCaptureViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionRepository: CaptureSessionRepository,
    private val photoDao: PhotoDao,
    private val visionAnalyzer: VisionAnalyzer,
    private val voiceJobParser: VoiceJobParser,
    private val fusionEngine: MultimodalFusionEngine,
    private val taxonomyCatalog: TaxonomyCatalog
) : ViewModel() {

    private val _ui = MutableStateFlow(FieldCaptureUiState())
    val ui: StateFlow<FieldCaptureUiState> = _ui.asStateFlow()

    private val predictionsByPhoto = linkedMapOf<String, List<VisionPrediction>>()

    val speciesOptions: List<Pair<String, String>>
        get() = taxonomyCatalog.species
            .filter { it.id !in setOf("unknown", "none") }
            .map { it.id to it.displayName }

    val damageOptions: List<Pair<String, String>>
        get() = taxonomyCatalog.damage
            .filter { it.id !in setOf("unknown", "none") }
            .map { it.id to it.displayName }

    fun start(sessionId: String? = null) {
        if (_ui.value.ready && sessionId.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                val session = if (!sessionId.isNullOrBlank()) {
                    sessionRepository.getSession(sessionId)
                        ?: sessionRepository.createSession()
                } else {
                    sessionRepository.createSession()
                }
                val draft = sessionRepository.decodeDraft(session)
                _ui.update {
                    it.copy(
                        sessionId = session.id,
                        transcript = session.voiceTranscript,
                        gpsLat = session.latitude,
                        gpsLon = session.longitude,
                        gpsAccuracy = session.accuracyMeters,
                        addressGuess = session.addressGuess,
                        draft = draft.copy(photoIds = draft.photoIds),
                        photos = draft.photoIds.map { id ->
                            FieldCapturePhotoUi(id, "", id.take(8))
                        },
                        phase = when (session.status) {
                            CaptureSessionStatus.REVIEW, CaptureSessionStatus.COMMITTED ->
                                FieldCapturePhase.REVIEW
                            else -> FieldCapturePhase.CAPTURE
                        },
                        warnings = draft.fusionWarnings,
                        ready = true,
                        error = session.errorMessage.ifBlank { null }
                    )
                }
                // Load photo paths for existing ids
                val photoUis = mutableListOf<FieldCapturePhotoUi>()
                for (id in draft.photoIds) {
                    val p = photoDao.getById(id)
                    if (p != null) {
                        photoUis += FieldCapturePhotoUi(
                            photoId = p.id,
                            localPath = p.localPath.ifBlank { p.filePath },
                            displayName = File(p.localPath.ifBlank { p.filePath }).name.ifBlank { p.id.take(8) }
                        )
                    }
                }
                if (photoUis.isNotEmpty()) {
                    _ui.update { it.copy(photos = photoUis) }
                }
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(
                        error = "Could not start capture: ${t.message ?: t.javaClass.simpleName}",
                        ready = true
                    )
                }
            }
        }
    }

    fun updateTranscript(text: String) {
        _ui.update { it.copy(transcript = text, error = null) }
    }

    fun appendSpeechResult(text: String) {
        if (text.isBlank()) return
        _ui.update { state ->
            val merged = when {
                state.transcript.isBlank() -> text
                state.transcript.endsWith(text) -> state.transcript
                else -> "${state.transcript.trim()} $text".trim()
            }
            state.copy(transcript = merged, error = null)
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    fun clearInfo() = _ui.update { it.copy(infoMessage = null) }
    fun clearCommittedJob() = _ui.update { it.copy(committedJobId = null) }

    fun backToCapture() {
        _ui.update { it.copy(phase = FieldCapturePhase.CAPTURE, error = null) }
    }

    fun addPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            val sessionId = _ui.value.sessionId.ifBlank { return@launch }
            try {
                val photoId = UUID.randomUUID().toString()
                val file = withContext(Dispatchers.IO) {
                    copyUriToCaptureFile(uri, sessionId, photoId)
                }
                val photo = Photo(
                    id = photoId,
                    filePath = file.absolutePath,
                    localPath = file.absolutePath,
                    captureSessionId = sessionId,
                    category = PhotoCategory.JOB_SITE,
                    fileSize = file.length(),
                    description = "Field capture"
                )
                photoDao.insert(photo)
                _ui.update { state ->
                    state.copy(
                        photos = state.photos + FieldCapturePhotoUi(
                            photoId = photoId,
                            localPath = file.absolutePath,
                            displayName = file.name
                        ),
                        draft = state.draft.copy(photoIds = state.draft.photoIds + photoId),
                        infoMessage = "Photo added"
                    )
                }
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(error = "Could not add photo: ${t.message ?: t.javaClass.simpleName}")
                }
            }
        }
    }

    fun addPhotoFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val sessionId = _ui.value.sessionId.ifBlank { return@launch }
            try {
                val photoId = UUID.randomUUID().toString()
                val file = withContext(Dispatchers.IO) {
                    val dir = File(appContext.filesDir, "captures/$sessionId").apply { mkdirs() }
                    val out = File(dir, "$photoId.jpg")
                    FileOutputStream(out).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    }
                    out
                }
                val photo = Photo(
                    id = photoId,
                    filePath = file.absolutePath,
                    localPath = file.absolutePath,
                    captureSessionId = sessionId,
                    category = PhotoCategory.JOB_SITE,
                    fileSize = file.length(),
                    description = "Field capture camera"
                )
                photoDao.insert(photo)
                _ui.update { state ->
                    state.copy(
                        photos = state.photos + FieldCapturePhotoUi(
                            photoId = photoId,
                            localPath = file.absolutePath,
                            displayName = file.name
                        ),
                        draft = state.draft.copy(photoIds = state.draft.photoIds + photoId),
                        infoMessage = "Photo captured"
                    )
                }
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(error = "Camera save failed: ${t.message ?: t.javaClass.simpleName}")
                }
            }
        }
    }

    fun removePhoto(photoId: String) {
        viewModelScope.launch {
            photoDao.deleteById(photoId)
            predictionsByPhoto.remove(photoId)
            _ui.update { state ->
                state.copy(
                    photos = state.photos.filterNot { it.photoId == photoId },
                    draft = state.draft.copy(photoIds = state.draft.photoIds.filterNot { it == photoId })
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshGps() {
        viewModelScope.launch {
            _ui.update { it.copy(isRefreshingGps = true, error = null) }
            try {
                val client = LocationServices.getFusedLocationProviderClient(appContext)
                val cts = CancellationTokenSource()
                val location = try {
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                } catch (_: Exception) {
                    client.lastLocation.await()
                }
                if (location == null) {
                    _ui.update {
                        it.copy(
                            isRefreshingGps = false,
                            error = "No GPS fix yet. Enable location and try again."
                        )
                    }
                    return@launch
                }
                val address = reverseGeocode(location.latitude, location.longitude)
                _ui.update {
                    it.copy(
                        isRefreshingGps = false,
                        gpsLat = location.latitude,
                        gpsLon = location.longitude,
                        gpsAccuracy = location.accuracy,
                        addressGuess = address.ifBlank { it.addressGuess },
                        infoMessage = "GPS updated (±${location.accuracy.toInt()} m)"
                    )
                }
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(
                        isRefreshingGps = false,
                        error = "GPS failed: ${t.message ?: t.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun analyzeAndContinue() {
        viewModelScope.launch {
            val state = _ui.value
            val sessionId = state.sessionId.ifBlank {
                _ui.update { it.copy(error = "Session not ready") }
                return@launch
            }
            if (state.transcript.isBlank() && state.photos.isEmpty()) {
                _ui.update {
                    it.copy(error = "Add a voice note and/or at least one photo before analyzing")
                }
                return@launch
            }
            _ui.update { it.copy(phase = FieldCapturePhase.ANALYZING, error = null) }
            try {
                val voice = voiceJobParser.parse(
                    state.transcript,
                    DraftHints(
                        knownAddress = state.addressGuess,
                        knownCustomerName = state.draft.customerName
                    )
                )
                val allPredictions = mutableListOf<VisionPrediction>()
                for (photo in state.photos) {
                    val uri = Uri.fromFile(File(photo.localPath))
                    val result = visionAnalyzer.analyze(
                        context = appContext,
                        photoUri = uri,
                        photoId = photo.photoId,
                        captureSessionId = sessionId
                    )
                    predictionsByPhoto[photo.photoId] = result.predictions
                    allPredictions += result.predictions
                    if (result.ok) {
                        photoDao.getById(photo.photoId)?.let { existing ->
                            photoDao.update(
                                existing.copy(
                                    visionAnalyzedAt = System.currentTimeMillis(),
                                    primarySpeciesLabelId = result.primarySpeciesLabelId,
                                    primaryDamageLabelId = result.primaryDamageLabelId,
                                    visionSummaryJson = "{\"service\":\"${result.serviceType}\",\"priority\":\"${result.priority}\"}"
                                )
                            )
                        }
                    }
                }
                sessionRepository.savePredictions(allPredictions)

                val gps = if (state.gpsLat != null && state.gpsLon != null) {
                    GpsFix(
                        latitude = state.gpsLat,
                        longitude = state.gpsLon,
                        accuracyMeters = state.gpsAccuracy,
                        addressGuess = state.addressGuess
                    )
                } else null

                val fused = fusionEngine.fuse(
                    voice = voice,
                    visions = allPredictions,
                    gps = gps,
                    existingDraft = state.draft.copy(
                        photoIds = state.photos.map { it.photoId },
                        notes = state.transcript.ifBlank { state.draft.notes }
                    ),
                    lockUserFields = true
                )

                sessionRepository.saveDraft(
                    sessionId = sessionId,
                    draft = fused,
                    status = CaptureSessionStatus.REVIEW,
                    voiceTranscript = state.transcript,
                    latitude = state.gpsLat,
                    longitude = state.gpsLon,
                    accuracyMeters = state.gpsAccuracy,
                    addressGuess = state.addressGuess.ifBlank { fused.address }
                )

                _ui.update {
                    it.copy(
                        phase = FieldCapturePhase.REVIEW,
                        draft = fused,
                        warnings = fused.fusionWarnings,
                        addressGuess = state.addressGuess.ifBlank { fused.address },
                        infoMessage = "Review the fused draft, then save"
                    )
                }
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(
                        phase = FieldCapturePhase.CAPTURE,
                        error = "Analyze failed: ${t.message ?: t.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun updateTitle(value: String) = patchDraft { it.copy(title = value) }
    fun updateCustomerName(value: String) = patchDraft { it.copy(customerName = value) }
    fun updateAddress(value: String) = patchDraft { it.copy(address = value) }
    fun updateFindings(value: String) = patchDraft { it.copy(findings = value) }
    fun updateRecommendations(value: String) = patchDraft { it.copy(recommendations = value) }
    fun updateEntryPoints(value: String) = patchDraft { it.copy(entryPoints = value) }
    fun updateNotes(value: String) = patchDraft { it.copy(notes = value) }
    fun updateServiceType(value: String) = patchDraft { it.copy(serviceType = value) }
    fun updatePriority(value: String) = patchDraft { it.copy(priority = value) }
    fun updateSeverity(value: Int) = patchDraft { it.copy(severity = value.coerceIn(0, 4)) }
    fun updatePriceLow(value: String) {
        val v = value.toDoubleOrNull() ?: return
        patchDraft { it.copy(estimatedPriceLow = v) }
    }
    fun updatePriceHigh(value: String) {
        val v = value.toDoubleOrNull() ?: return
        patchDraft { it.copy(estimatedPriceHigh = v) }
    }

    fun toggleSpecies(labelId: String, displayName: String) {
        patchDraft { draft ->
            val existing = draft.speciesLabelIds.firstOrNull { it.labelId == labelId }
            val next = if (existing != null) {
                draft.speciesLabelIds.filterNot { it.labelId == labelId }
            } else {
                draft.speciesLabelIds + ScoredLabel(
                    labelId = labelId,
                    displayLabel = displayName,
                    confidence = 1f,
                    provenance = FieldProvenance.USER,
                    accepted = true
                )
            }
            draft.copy(
                speciesLabelIds = next,
                fieldProvenance = draft.fieldProvenance + ("species" to FieldProvenance.USER)
            )
        }
    }

    fun toggleDamage(labelId: String, displayName: String) {
        patchDraft { draft ->
            val existing = draft.damageLabelIds.firstOrNull { it.labelId == labelId }
            val next = if (existing != null) {
                draft.damageLabelIds.filterNot { it.labelId == labelId }
            } else {
                draft.damageLabelIds + ScoredLabel(
                    labelId = labelId,
                    displayLabel = displayName,
                    confidence = 1f,
                    provenance = FieldProvenance.USER,
                    accepted = true
                )
            }
            draft.copy(
                damageLabelIds = next,
                fieldProvenance = draft.fieldProvenance + ("damage" to FieldProvenance.USER)
            )
        }
    }

    fun saveJob() {
        viewModelScope.launch {
            val state = _ui.value
            val sessionId = state.sessionId.ifBlank {
                _ui.update { it.copy(error = "No session") }
                return@launch
            }
            _ui.update { it.copy(phase = FieldCapturePhase.SAVING, error = null) }
            // Persist latest draft edits before commit
            sessionRepository.saveDraft(
                sessionId = sessionId,
                draft = state.draft,
                status = CaptureSessionStatus.REVIEW,
                voiceTranscript = state.transcript,
                latitude = state.gpsLat,
                longitude = state.gpsLon,
                accuracyMeters = state.gpsAccuracy,
                addressGuess = state.addressGuess
            )
            val allPreds = predictionsByPhoto.values.flatten()
            val result = sessionRepository.commit(
                CaptureCommitRequest(
                    sessionId = sessionId,
                    reviewedDraft = state.draft.copy(photoIds = state.photos.map { it.photoId }),
                    predictions = allPreds,
                    latitude = state.gpsLat,
                    longitude = state.gpsLon,
                    technicianName = "",
                    autoLabelsFromDraft = true
                )
            )
            when (result) {
                is CaptureCommitResult.Success -> {
                    _ui.update {
                        it.copy(
                            phase = FieldCapturePhase.REVIEW,
                            committedJobId = result.jobId,
                            infoMessage = "Saved job · ${result.trainingLabelCount} training labels"
                        )
                    }
                }
                is CaptureCommitResult.Failure -> {
                    _ui.update {
                        it.copy(
                            phase = FieldCapturePhase.REVIEW,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            val sessionId = _ui.value.sessionId
            if (sessionId.isNotBlank()) {
                sessionRepository.discard(sessionId)
            }
            onDone()
        }
    }

    private fun patchDraft(transform: (MultimodalDraftSnapshot) -> MultimodalDraftSnapshot) {
        _ui.update { state ->
            val next = transform(state.draft)
            state.copy(draft = next, warnings = next.fusionWarnings)
        }
        // Persist asynchronously so review survives process death
        val snapshot = _ui.value
        if (snapshot.sessionId.isBlank()) return
        viewModelScope.launch {
            sessionRepository.saveDraft(
                sessionId = snapshot.sessionId,
                draft = snapshot.draft,
                status = CaptureSessionStatus.REVIEW,
                voiceTranscript = snapshot.transcript,
                latitude = snapshot.gpsLat,
                longitude = snapshot.gpsLon,
                accuracyMeters = snapshot.gpsAccuracy,
                addressGuess = snapshot.addressGuess
            )
        }
    }

    private fun copyUriToCaptureFile(uri: Uri, sessionId: String, photoId: String): File {
        val dir = File(appContext.filesDir, "captures/$sessionId").apply { mkdirs() }
        val out = File(dir, "$photoId.jpg")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        } ?: error("Cannot open image")
        return out
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching ""
                @Suppress("DEPRECATION")
                val list = Geocoder(appContext, Locale.getDefault()).getFromLocation(lat, lon, 1)
                list?.firstOrNull()?.getAddressLine(0).orEmpty()
            }.getOrDefault("")
        }
}
