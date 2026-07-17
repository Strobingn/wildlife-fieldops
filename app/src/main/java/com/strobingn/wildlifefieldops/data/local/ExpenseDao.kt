package com.strobingn.wildlife.data.local

import androidx.room.*
import com.strobingn.wildlife.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun getAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE jobId = :jobId ORDER BY expenseDate DESC")
    fun getByJob(jobId: String): Flow<List<Expense>>

    @Query("SELECT SUM(totalAmount) FROM expenses WHERE expenseDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalForPeriod(startDate: Long, endDate: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}
