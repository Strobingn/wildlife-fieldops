package com.strobingn.wildlifefieldops.data.repository

import com.strobingn.wildlifefieldops.data.local.CaptureSessionDao
import com.strobingn.wildlifefieldops.data.local.VisionPredictionDao
import com.strobingn.wildlifefieldops.data.model.CaptureSession
import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitRequest
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitResult
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitter
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for Field Capture session lifecycle + commit.
 * UI / ViewModels should prefer this over talking to DAOs and committer separately.
 */
@Singleton
class CaptureSessionRepository @Inject constructor(
    private val captureSessionDao: CaptureSessionDao,
    private val visionPredictionDao: VisionPredictionDao,
    private val captureCommitter: CaptureCommitter
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createSession(
        voiceTranscript: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        accuracyMeters: Float? = null,
        addressGuess: String = ""
    ): CaptureSession {
        val session = CaptureSession(
            status = CaptureSessionStatus.DRAFT,
            voiceTranscript = voiceTranscript,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            addressGuess = addressGuess,
            draftJson = "{}"
        )
        captureSessionDao.insert(session)
        return session
    }

    suspend fun getSession(sessionId: String): CaptureSession? =
        captureSessionDao.getById(sessionId)

    fun observeSession(sessionId: String): Flow<CaptureSession?> =
        captureSessionDao.observeById(sessionId)

    fun observeOpenSessions(limit: Int = 20): Flow<List<CaptureSession>> =
        captureSessionDao.observeOpenSessions(limit)

    suspend fun saveDraft(
        sessionId: String,
        draft: MultimodalDraftSnapshot,
        status: CaptureSessionStatus = CaptureSessionStatus.REVIEW,
        voiceTranscript: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        accuracyMeters: Float? = null,
        addressGuess: String? = null
    ): CaptureSession? {
        val existing = captureSessionDao.getById(sessionId) ?: return null
        if (existing.status == CaptureSessionStatus.COMMITTED ||
            existing.status == CaptureSessionStatus.DISCARDED
        ) {
            return existing
        }
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = status,
            draftJson = json.encodeToString(draft),
            voiceTranscript = voiceTranscript ?: existing.voiceTranscript,
            latitude = latitude ?: existing.latitude,
            longitude = longitude ?: existing.longitude,
            accuracyMeters = accuracyMeters ?: existing.accuracyMeters,
            addressGuess = addressGuess ?: existing.addressGuess,
            errorMessage = "",
            updatedAt = now
        )
        captureSessionDao.update(updated)
        return updated
    }

    suspend fun savePredictions(predictions: List<VisionPrediction>) {
        if (predictions.isNotEmpty()) {
            visionPredictionDao.insertAll(predictions)
        }
    }

    suspend fun discard(sessionId: String): CaptureSession? {
        val existing = captureSessionDao.getById(sessionId) ?: return null
        if (existing.status == CaptureSessionStatus.COMMITTED) return existing
        val updated = existing.copy(
            status = CaptureSessionStatus.DISCARDED,
            updatedAt = System.currentTimeMillis(),
            errorMessage = ""
        )
        captureSessionDao.update(updated)
        return updated
    }

    suspend fun commit(request: CaptureCommitRequest): CaptureCommitResult =
        captureCommitter.commit(request)

    fun decodeDraft(session: CaptureSession): MultimodalDraftSnapshot {
        return runCatching {
            json.decodeFromString(MultimodalDraftSnapshot.serializer(), session.draftJson)
        }.getOrDefault(MultimodalDraftSnapshot())
    }
}
