package com.strobingn.wildlifefieldops.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.strobingn.wildlifefieldops.data.model.ContractStatus
import com.strobingn.wildlifefieldops.data.model.DigitalContract
import kotlinx.coroutines.flow.Flow

@Dao
interface DigitalContractDao {
    @Query("SELECT * FROM digital_contracts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DigitalContract>>

    @Query("SELECT * FROM digital_contracts WHERE id = :id")
    suspend fun getById(id: String): DigitalContract?

    @Query("SELECT * FROM digital_contracts WHERE id = :id")
    fun observeById(id: String): Flow<DigitalContract?>

    @Query("SELECT * FROM digital_contracts WHERE jobId = :jobId ORDER BY createdAt DESC")
    fun getByJob(jobId: String): Flow<List<DigitalContract>>

    @Query("SELECT * FROM digital_contracts WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getByCustomer(customerId: String): Flow<List<DigitalContract>>

    @Query("SELECT * FROM digital_contracts WHERE status = :status ORDER BY updatedAt DESC")
    fun getByStatus(status: ContractStatus): Flow<List<DigitalContract>>

    @Query("SELECT * FROM digital_contracts WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DigitalContract>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contract: DigitalContract)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contracts: List<DigitalContract>)

    @Update
    suspend fun update(contract: DigitalContract)

    @Delete
    suspend fun delete(contract: DigitalContract)

    @Query("UPDATE digital_contracts SET isSynced = 1, syncError = NULL WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM digital_contracts WHERE id = :id")
    suspend fun deleteById(id: String)
}
