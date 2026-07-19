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
        PendingOperation::class
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

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // v1 shipped in two flavors: early builds lack inspections.notes, later v1 builds have it.
                var hasNotes = false
                database.query("PRAGMA table_info(inspections)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0 && cursor.getString(nameIndex) == "notes") {
                            hasNotes = true
                            break
                        }
                    }
                }
                if (!hasNotes) {
                    database.execSQL("ALTER TABLE inspections ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                }
            }
        }

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE jobs ADD COLUMN warrantyMonths INTEGER NOT NULL DEFAULT 12")
            }
        }
    }
}
