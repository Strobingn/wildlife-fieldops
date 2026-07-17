package com.strobingn.wildlifefieldops.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.strobingn.wildlifefieldops.data.model.Inspection
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY inspectionDate DESC")
    fun getAll(): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Inspection?

    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Inspection?>

    @Query("SELECT * FROM inspections WHERE jobId = :jobId ORDER BY inspectionDate DESC")
    fun getByJob(jobId: String): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE customerId = :customerId ORDER BY inspectionDate DESC")
    fun getByCustomer(customerId: String): Flow<List<Inspection>>

    @Query(
        """
        SELECT * FROM inspections
        WHERE inspectionDate >= :startInclusive
          AND inspectionDate < :endExclusive
        ORDER BY inspectionDate ASC
        """
    )
    fun getScheduledBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<Inspection>>

    @Query(
        """
        SELECT * FROM inspections
        WHERE followUpRequired = 1
          AND followUpDate IS NOT NULL
          AND followUpDate >= :startInclusive
          AND followUpDate < :endExclusive
        ORDER BY followUpDate ASC
        """
    )
    fun getFollowUpsBetween(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE isSynced = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsynced(): List<Inspection>

    @Query("SELECT * FROM inspections WHERE followUpRequired = 1 AND followUpDate <= :currentTime ORDER BY followUpDate ASC")
    suspend fun getPendingFollowUps(currentTime: Long): List<Inspection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inspection: Inspection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inspections: List<Inspection>)

    @Update
    suspend fun update(inspection: Inspection)

    @Delete
    suspend fun delete(inspection: Inspection)

    @Query("DELETE FROM inspections WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM inspections")
    suspend fun count(): Int

    @Query("UPDATE inspections SET isSynced = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(
        id: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM inspections")
    suspend fun deleteAll()
}
