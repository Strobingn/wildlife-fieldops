package com.strobingn.wildlifefieldops.ml.commit

import androidx.room.withTransaction
import com.strobingn.wildlifefieldops.data.local.AppDatabase
import com.strobingn.wildlifefieldops.data.local.CaptureSessionDao
import com.strobingn.wildlifefieldops.data.local.InspectionDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.local.PhotoDao
import com.strobingn.wildlifefieldops.data.local.TrainingLabelDao
import com.strobingn.wildlifefieldops.data.local.VisionPredictionDao
import com.strobingn.wildlifefieldops.data.model.CaptureSession
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.data.model.InspectionType
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.data.model.Photo
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitRequest
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitResult
import com.strobingn.wildlifefieldops.ml.domain.CaptureCommitter
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transactional capture commit per DESIGN.md §4.5.
 * On failure: session stays non-committed (REVIEW) with [CaptureSession.errorMessage]; no partial job.
 */
@Singleton
class RoomCaptureCommitter @Inject constructor(
    private val database: AppDatabase,
    private val captureSessionDao: CaptureSessionDao,
    private val jobDao: JobDao,
    private val inspectionDao: InspectionDao,
    private val photoDao: PhotoDao,
    private val visionPredictionDao: VisionPredictionDao,
    private val trainingLabelDao: TrainingLabelDao
) : CaptureCommitter {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun commit(request: CaptureCommitRequest): CaptureCommitResult {
        val session = captureSessionDao.getById(request.sessionId)
            ?: return CaptureCommitResult.Failure(
                message = "Capture session not found: ${request.sessionId}",
                sessionId = request.sessionId
            )

        val statusCheck = CaptureCommitValidator.validateSessionStatus(session.status)
        if (!statusCheck.ok) {
            return failSession(session, statusCheck.message)
        }

        val draft = request.reviewedDraft
        val contentCheck = CaptureCommitValidator.validateDraftContent(draft)
        if (!contentCheck.ok) {
            return failSession(session.copy(status = CaptureSessionStatus.REVIEW), contentCheck.message)
        }

        return try {
            database.withTransaction {
                persistAtomic(session, request, draft)
            }
        } catch (t: Throwable) {
            failSession(
                session.copy(status = CaptureSessionStatus.REVIEW),
                "Commit failed: ${t.message ?: t.javaClass.simpleName}"
            )
        }
    }

    private suspend fun persistAtomic(
        session: CaptureSession,
        request: CaptureCommitRequest,
        draft: MultimodalDraftSnapshot
    ): CaptureCommitResult.Success {
        val now = System.currentTimeMillis()
        val jobId = UUID.randomUUID().toString()
        val inspectionId = UUID.randomUUID().toString()

        val speciesDisplay = draft.speciesLabelIds
            .filter { it.labelId !in IGNORE }
            .joinToString(", ") { it.displayLabel.ifBlank { it.labelId } }

        val lat = request.latitude ?: session.latitude
        val lon = request.longitude ?: session.longitude
        val midpoint = when {
            draft.estimatedPriceLow > 0 && draft.estimatedPriceHigh > 0 ->
                (draft.estimatedPriceLow + draft.estimatedPriceHigh) / 2.0
            draft.estimatedPriceLow > 0 -> draft.estimatedPriceLow
            draft.estimatedPriceHigh > 0 -> draft.estimatedPriceHigh
            else -> 0.0
        }

        val job = Job(
            id = jobId,
            title = draft.title.ifBlank {
                if (speciesDisplay.isNotBlank()) "$speciesDisplay job" else "Field capture job"
            },
            description = draft.findings.ifBlank { draft.notes },
            customerId = "",
            customerName = draft.customerName,
            address = draft.address.ifBlank { session.addressGuess },
            latitude = lat,
            longitude = lon,
            status = JobStatus.PENDING,
            priority = CaptureSeverityMapper.toJobPriority(draft.priority, draft.severity),
            type = draft.serviceType.ifBlank { "Inspection" },
            estimatedValue = midpoint,
            actualCost = 0.0,
            assignedTo = request.technicianName,
            createdAt = now,
            updatedAt = now,
            scheduledDate = null,
            completedDate = null,
            notes = draft.notes,
            photos = draft.photoIds,
            isSynced = false,
            syncError = null
        )
        jobDao.insert(job)

        val inspection = Inspection(
            id = inspectionId,
            jobId = jobId,
            customerId = "",
            customerName = draft.customerName,
            inspectorName = request.technicianName,
            inspectionType = InspectionType.ROUTINE,
            inspectionDate = now,
            findings = draft.findings.ifBlank { draft.notes },
            recommendations = draft.recommendations,
            severity = CaptureSeverityMapper.toFindingSeverity(draft.severity),
            speciesIdentified = speciesDisplay,
            entryPoints = draft.entryPoints,
            damageAssessment = draft.damageLabelIds
                .filter { it.labelId !in IGNORE }
                .joinToString(", ") { it.displayLabel.ifBlank { it.labelId } },
            photos = draft.photoIds,
            followUpRequired = draft.severity >= 3,
            followUpDate = if (draft.severity >= 3) now + 7L * 24 * 60 * 60 * 1000 else null,
            temperature = null,
            weatherConditions = "",
            notes = draft.notes,
            humidity = null,
            windSpeed = null,
            latitude = lat,
            longitude = lon,
            createdAt = now,
            updatedAt = now,
            isSynced = false
        )
        inspectionDao.insert(inspection)

        val primarySpecies = draft.speciesLabelIds
            .filter { it.labelId !in IGNORE }
            .maxByOrNull { it.confidence }
            ?.labelId
            .orEmpty()
        val primaryDamage = draft.damageLabelIds
            .filter { it.labelId !in IGNORE }
            .maxByOrNull { it.confidence }
            ?.labelId
            .orEmpty()

        val photoIds = draft.photoIds.distinct()
        for (photoId in photoIds) {
            val existing = photoDao.getById(photoId)
            if (existing != null) {
                photoDao.update(
                    existing.copy(
                        jobId = jobId,
                        inspectionId = inspectionId,
                        captureSessionId = session.id,
                        primarySpeciesLabelId = primarySpecies.ifBlank { existing.primarySpeciesLabelId },
                        primaryDamageLabelId = primaryDamage.ifBlank { existing.primaryDamageLabelId },
                        visionAnalyzedAt = existing.visionAnalyzedAt ?: now
                    )
                )
            } else {
                // Ensure linkage even if Photo row was not pre-created (path filled later by UI).
                photoDao.insert(
                    Photo(
                        id = photoId,
                        jobId = jobId,
                        inspectionId = inspectionId,
                        captureSessionId = session.id,
                        primarySpeciesLabelId = primarySpecies,
                        primaryDamageLabelId = primaryDamage,
                        visionAnalyzedAt = now,
                        description = "Field capture",
                        createdAt = now,
                        takenAt = now
                    )
                )
            }
        }

        val predictions = request.predictions.map { pred ->
            pred.copy(
                jobId = jobId,
                inspectionId = inspectionId,
                captureSessionId = session.id
            )
        }
        if (predictions.isNotEmpty()) {
            visionPredictionDao.insertAll(predictions)
        }

        val labelDrafts = when {
            request.labels.isNotEmpty() -> request.labels
            request.autoLabelsFromDraft && photoIds.isNotEmpty() ->
                TrainingLabelDraft.fromScoredLabels(
                    photoId = photoIds.first(),
                    species = draft.speciesLabelIds,
                    damage = draft.damageLabelIds,
                    createdBy = request.technicianName
                )
            request.autoLabelsFromDraft ->
                // Session-level labels without photo: attach to synthetic photo id = session id
                TrainingLabelDraft.fromScoredLabels(
                    photoId = session.id,
                    species = draft.speciesLabelIds,
                    damage = draft.damageLabelIds,
                    createdBy = request.technicianName
                )
            else -> emptyList()
        }
        val labelEntities = labelDrafts.map { it.toEntity(now) }
        if (labelEntities.isNotEmpty()) {
            trainingLabelDao.insertAll(labelEntities)
        }

        val updatedSession = session.copy(
            status = CaptureSessionStatus.COMMITTED,
            draftJson = json.encodeToString(draft),
            fusedJobId = jobId,
            fusedInspectionId = inspectionId,
            errorMessage = "",
            latitude = lat ?: session.latitude,
            longitude = lon ?: session.longitude,
            updatedAt = now,
            committedAt = now
        )
        captureSessionDao.update(updatedSession)

        return CaptureCommitResult.Success(
            jobId = jobId,
            inspectionId = inspectionId,
            sessionId = session.id,
            trainingLabelCount = labelEntities.size
        )
    }

    private suspend fun failSession(
        session: CaptureSession,
        message: String
    ): CaptureCommitResult.Failure {
        val now = System.currentTimeMillis()
        val status = when (session.status) {
            CaptureSessionStatus.COMMITTED, CaptureSessionStatus.DISCARDED -> session.status
            else -> CaptureSessionStatus.REVIEW
        }
        captureSessionDao.update(
            session.copy(
                status = status,
                errorMessage = message,
                updatedAt = now
            )
        )
        return CaptureCommitResult.Failure(message = message, sessionId = session.id)
    }

    companion object {
        private val IGNORE = setOf("unknown", "none", "")
    }
}
