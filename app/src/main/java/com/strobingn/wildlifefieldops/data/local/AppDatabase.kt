package com.strobingn.wildlifefieldops.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.strobingn.wildlifefieldops.data.model.*

@Database(
    entities = [
        Job::class,
        Customer::class,
        Inspection::class,
        Photo::class,
        Visit::class,
        Repair::class,
        Expense::class,
        TrapLog::class,
        InventoryItem::class,
        Reminder::class,
        Invoice::class,
        PendingOperation::class,
        VisionPrediction::class,
        TrainingLabel::class,
        CaptureSession::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun customerDao(): CustomerDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun photoDao(): PhotoDao
    abstract fun visitDao(): VisitDao
    abstract fun repairDao(): RepairDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun trapLogDao(): TrapLogDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun reminderDao(): ReminderDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun visionPredictionDao(): VisionPredictionDao
    abstract fun trainingLabelDao(): TrainingLabelDao
    abstract fun captureSessionDao(): CaptureSessionDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_operations` (
                        `id` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastError` TEXT NOT NULL,
                        `lastAttempt` INTEGER,
                        `isProcessing` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * ML P0: vision predictions, training labels, capture sessions, photo ML columns.
         * See F:\wildlife-fieldops\design\ml-p0\DATA-MODELS.md
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vision_predictions` (
                        `id` TEXT NOT NULL,
                        `photoId` TEXT NOT NULL,
                        `jobId` TEXT,
                        `inspectionId` TEXT,
                        `captureSessionId` TEXT,
                        `backend` TEXT NOT NULL,
                        `modelVersion` TEXT NOT NULL,
                        `target` TEXT NOT NULL,
                        `labelId` TEXT NOT NULL,
                        `displayLabel` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `boxLeft` REAL,
                        `boxTop` REAL,
                        `boxRight` REAL,
                        `boxBottom` REAL,
                        `rawLabelsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vision_predictions_photoId` ON `vision_predictions` (`photoId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vision_predictions_jobId` ON `vision_predictions` (`jobId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vision_predictions_captureSessionId` ON `vision_predictions` (`captureSessionId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vision_predictions_createdAt` ON `vision_predictions` (`createdAt`)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `training_labels` (
                        `id` TEXT NOT NULL,
                        `photoId` TEXT NOT NULL,
                        `visionPredictionId` TEXT,
                        `target` TEXT NOT NULL,
                        `labelId` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `modelLabelId` TEXT,
                        `modelConfidence` REAL,
                        `boxLeft` REAL,
                        `boxTop` REAL,
                        `boxRight` REAL,
                        `boxBottom` REAL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `exportedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_training_labels_photoId` ON `training_labels` (`photoId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_training_labels_labelId` ON `training_labels` (`labelId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_training_labels_createdAt` ON `training_labels` (`createdAt`)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `capture_sessions` (
                        `id` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `voiceTranscript` TEXT NOT NULL,
                        `voiceAudioPath` TEXT NOT NULL,
                        `latitude` REAL,
                        `longitude` REAL,
                        `accuracyMeters` REAL,
                        `addressGuess` TEXT NOT NULL,
                        `draftJson` TEXT NOT NULL,
                        `fusedJobId` TEXT,
                        `fusedInspectionId` TEXT,
                        `errorMessage` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `committedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_capture_sessions_status` ON `capture_sessions` (`status`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_capture_sessions_createdAt` ON `capture_sessions` (`createdAt`)"
                )

                database.execSQL("ALTER TABLE `photos` ADD COLUMN `captureSessionId` TEXT")
                database.execSQL("ALTER TABLE `photos` ADD COLUMN `visionAnalyzedAt` INTEGER")
                database.execSQL(
                    "ALTER TABLE `photos` ADD COLUMN `primarySpeciesLabelId` TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE `photos` ADD COLUMN `primaryDamageLabelId` TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE `photos` ADD COLUMN `visionSummaryJson` TEXT NOT NULL DEFAULT '{}'"
                )
            }
        }
    }
}
