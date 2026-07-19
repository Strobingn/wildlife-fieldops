package com.strobingn.wildlifefieldops.ml.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.strobingn.wildlifefieldops.data.local.TrainingLabelDao
import com.strobingn.wildlifefieldops.data.model.TrainingLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TrainingLabelExportLine(
    val id: String,
    val photoId: String,
    val visionPredictionId: String? = null,
    val target: String,
    val labelId: String,
    val source: String,
    val modelLabelId: String? = null,
    val modelConfidence: Float? = null,
    val notes: String = "",
    val createdAt: Long,
    val createdBy: String = ""
)

sealed class ExportResult {
    data class Success(
        val file: File,
        val lineCount: Int,
        val shareIntent: Intent
    ) : ExportResult()

    data class Empty(val message: String = "No training labels to export") : ExportResult()

    data class Failure(val message: String) : ExportResult()
}

/**
 * Writes training labels as JSONL and builds a FileProvider share intent.
 */
@Singleton
class TrainingLabelExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trainingLabelDao: TrainingLabelDao
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * @param unexportedOnly when true, only labels with exportedAt == null
     * @param markExported when true, stamp exportedAt on written rows
     */
    suspend fun exportJsonl(
        unexportedOnly: Boolean = false,
        markExported: Boolean = true,
        limit: Int = 5000
    ): ExportResult {
        return try {
            val toWrite: List<TrainingLabel> = if (unexportedOnly) {
                trainingLabelDao.getUnexported(limit)
            } else {
                trainingLabelDao.getAll(limit)
            }

            if (toWrite.isEmpty()) return ExportResult.Empty()

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "training_labels_$stamp.jsonl")

            file.bufferedWriter().use { writer ->
                for (label in toWrite) {
                    val line = TrainingLabelExportLine(
                        id = label.id,
                        photoId = label.photoId,
                        visionPredictionId = label.visionPredictionId,
                        target = label.target.name,
                        labelId = label.labelId,
                        source = label.source.name,
                        modelLabelId = label.modelLabelId,
                        modelConfidence = label.modelConfidence,
                        notes = label.notes,
                        createdAt = label.createdAt,
                        createdBy = label.createdBy
                    )
                    writer.appendLine(json.encodeToString(line))
                }
            }

            if (markExported) {
                trainingLabelDao.markExported(toWrite.map { it.id })
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/x-ndjson"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Wildlife FieldOps training labels")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Exported ${toWrite.size} training label(s) from Wildlife FieldOps ML P0."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            ExportResult.Success(
                file = file,
                lineCount = toWrite.size,
                shareIntent = Intent.createChooser(share, "Share training labels")
            )
        } catch (t: Throwable) {
            ExportResult.Failure(t.message ?: t.javaClass.simpleName)
        }
    }
}
