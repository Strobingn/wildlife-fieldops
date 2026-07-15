package com.strobingn.wildlifefieldops.data.local

import androidx.room.*
import com.strobingn.wildlifefieldops.data.model.Invoice
import com.strobingn.wildlifefieldops.data.model.InvoiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): Invoice?

    @Query("SELECT * FROM invoices WHERE jobId = :jobId")
    fun getByJob(jobId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: InvoiceStatus): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<Invoice>)

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int

    // --- Sync support ---

    @Query("SELECT * FROM invoices WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Invoice>

    @Query("UPDATE invoices SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE invoices SET isSynced = 0 WHERE id = :id")
    suspend fun markUnsynced(id: String)

    @Query("UPDATE invoices SET syncError = :error WHERE id = :id")
    suspend fun setSyncError(id: String, error: String?)

    @Query("SELECT COUNT(*) FROM invoices WHERE isSynced = 0")
    suspend fun unsyncedCount(): Int
}
