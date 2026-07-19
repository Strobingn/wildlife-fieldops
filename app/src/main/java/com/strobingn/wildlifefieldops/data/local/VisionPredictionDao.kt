package com.strobingn.wildlifefieldops.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import kotlinx.coroutines.flow.Flow

@Dao
interface VisionPredictionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prediction: VisionPrediction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(predictions: List<VisionPrediction>)

    @Query("SELECT * FROM vision_predictions WHERE photoId = :photoId ORDER BY confidence DESC")
    suspend fun getByPhoto(photoId: String): List<VisionPrediction>

    @Query("SELECT * FROM vision_predictions WHERE photoId = :photoId ORDER BY confidence DESC")
    fun observeByPhoto(photoId: String): Flow<List<VisionPrediction>>

    @Query(
        "SELECT * FROM vision_predictions WHERE captureSessionId = :sessionId ORDER BY createdAt ASC"
    )
    suspend fun getBySession(sessionId: String): List<VisionPrediction>

    @Query(
        "SELECT * FROM vision_predictions WHERE captureSessionId = :sessionId ORDER BY createdAt ASC"
    )
    fun observeBySession(sessionId: String): Flow<List<VisionPrediction>>

    @Query("SELECT * FROM vision_predictions WHERE jobId = :jobId ORDER BY createdAt DESC")
    suspend fun getByJob(jobId: String): List<VisionPrediction>

    @Query("DELETE FROM vision_predictions WHERE photoId = :photoId")
    suspend fun deleteByPhoto(photoId: String)

    @Query("DELETE FROM vision_predictions WHERE captureSessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("SELECT COUNT(*) FROM vision_predictions")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vision_predictions")
    suspend fun countAll(): Int
}
