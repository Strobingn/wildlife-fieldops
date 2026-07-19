package com.strobingn.wildlifefieldops.data.local

import androidx.room.*
import com.strobingn.wildlifefieldops.data.model.EntityType
import com.strobingn.wildlifefieldops.data.model.PendingOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PendingOperation>>

    @Query("SELECT * FROM pending_operations WHERE isProcessing = 0 ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingOperation>

    @Query("SELECT * FROM pending_operations WHERE isProcessing = 1")
    suspend fun getProcessing(): List<PendingOperation>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE isProcessing = 0")
    suspend fun getPendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<PendingOperation>)

    @Update
    suspend fun update(operation: PendingOperation)

    @Delete
    suspend fun delete(operation: PendingOperation)

    @Query("SELECT * FROM pending_operations WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun findByEntity(entityType: EntityType, entityId: String): PendingOperation?

    @Query("DELETE FROM pending_operations WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: EntityType, entityId: String)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()

    @Query("UPDATE pending_operations SET retryCount = retryCount + 1, lastError = :error, lastAttempt = :timestamp, isProcessing = 0 WHERE id = :id")
    suspend fun markFailed(id: String, error: String, timestamp: Long)

    @Query("UPDATE pending_operations SET isProcessing = 1 WHERE id = :id")
    suspend fun markProcessing(id: String)

    @Query("UPDATE pending_operations SET isProcessing = 0 WHERE isProcessing = 1")
    suspend fun resetProcessing()
}
