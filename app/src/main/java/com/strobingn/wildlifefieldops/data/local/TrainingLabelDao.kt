package com.strobingn.wildlifefieldops.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.strobingn.wildlifefieldops.data.model.TrainingLabel
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingLabelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(label: TrainingLabel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(labels: List<TrainingLabel>)

    @Update
    suspend fun update(label: TrainingLabel)

    @Query("SELECT * FROM training_labels WHERE photoId = :photoId ORDER BY createdAt DESC")
    suspend fun getByPhoto(photoId: String): List<TrainingLabel>

    @Query(
        """
        SELECT * FROM training_labels
        WHERE exportedAt IS NULL
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getUnexported(limit: Int = 500): List<TrainingLabel>

    @Query(
        """
        UPDATE training_labels
        SET exportedAt = :exportedAt
        WHERE id IN (:ids)
        """
    )
    suspend fun markExported(ids: List<String>, exportedAt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT labelId, COUNT(*) as cnt FROM training_labels
        GROUP BY labelId
        ORDER BY cnt DESC
        """
    )
    suspend fun countByLabel(): List<LabelCountRow>

    @Query("SELECT COUNT(*) FROM training_labels")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM training_labels WHERE exportedAt IS NULL")
    suspend fun countUnexported(): Int
}

/** Projection for [TrainingLabelDao.countByLabel]. */
data class LabelCountRow(
    val labelId: String,
    val cnt: Int
)
