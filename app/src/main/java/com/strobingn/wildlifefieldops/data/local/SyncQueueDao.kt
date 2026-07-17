package com.strobingn.wildlifefieldops.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.strobingn.wildlifefieldops.data.model.SyncQueueItem
import com.strobingn.wildlifefieldops.data.model.SyncQueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status != 'COMPLETED' ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<SyncQueueItem>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status != 'COMPLETED'")
    fun observePendingCount(): Flow<Int>

    @Query(
        "SELECT * FROM sync_queue " +
            "WHERE status IN ('PENDING', 'FAILED') AND nextAttemptAt <= :now " +
            "ORDER BY createdAt ASC LIMIT :limit"
    )
    suspend fun getReady(now: Long = System.currentTimeMillis(), limit: Int = 100): List<SyncQueueItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncQueueItem>)

    @Query(
        "UPDATE sync_queue SET status = :status, updatedAt = :updatedAt " +
            "WHERE id IN (:ids)"
    )
    suspend fun updateStatus(
        ids: List<String>,
        status: SyncQueueStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        "UPDATE sync_queue SET status = 'FAILED', attemptCount = attemptCount + 1, " +
            "lastError = :error, nextAttemptAt = :nextAttemptAt, updatedAt = :updatedAt " +
            "WHERE id IN (:ids)"
    )
    suspend fun markFailed(
        ids: List<String>,
        error: String,
        nextAttemptAt: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        "UPDATE sync_queue SET status = 'COMPLETED', lastError = NULL, " +
            "updatedAt = :updatedAt WHERE id IN (:ids)"
    )
    suspend fun markCompleted(ids: List<String>, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED' AND updatedAt < :olderThan")
    suspend fun pruneCompleted(olderThan: Long): Int

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
