package com.strobingn.wildlifefieldops.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.strobingn.wildlifefieldops.data.model.CaptureSession
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: CaptureSession)

    @Update
    suspend fun update(session: CaptureSession)

    @Query("SELECT * FROM capture_sessions WHERE id = :id")
    suspend fun getById(id: String): CaptureSession?

    @Query("SELECT * FROM capture_sessions WHERE id = :id")
    fun observeById(id: String): Flow<CaptureSession?>

    @Query(
        """
        SELECT * FROM capture_sessions
        WHERE status = :status
        ORDER BY updatedAt DESC
        """
    )
    fun observeByStatus(status: CaptureSessionStatus): Flow<List<CaptureSession>>

    @Query(
        """
        SELECT * FROM capture_sessions
        WHERE status IN ('DRAFT', 'REVIEW')
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    fun observeOpenSessions(limit: Int = 20): Flow<List<CaptureSession>>

    @Query("SELECT * FROM capture_sessions ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<CaptureSession>

    @Query("DELETE FROM capture_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM capture_sessions WHERE status = :status")
    suspend fun countByStatus(status: CaptureSessionStatus): Int

    @Query(
        """
        SELECT * FROM capture_sessions
        WHERE errorMessage != ''
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLastWithError(): CaptureSession?

    @Query("SELECT COUNT(*) FROM capture_sessions")
    suspend fun countAll(): Int
}
