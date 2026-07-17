package com.strobingn.wildlife.data.repository

import com.strobingn.wildlife.data.local.JobDao
import com.strobingn.wildlife.data.model.Job
import com.strobingn.wildlife.data.model.JobStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobDao: JobDao
) {
    fun getAllJobs(): Flow<List<Job>> = jobDao.getAll()

    fun getJobsByStatus(status: JobStatus): Flow<List<Job>> = jobDao.getByStatus(status)

    suspend fun getJobById(id: String): Job? = jobDao.getById(id)

    suspend fun saveJob(job: Job) = jobDao.insert(job)

    suspend fun updateJob(job: Job) = jobDao.update(job)

    suspend fun deleteJob(job: Job) = jobDao.delete(job)

    fun searchJobs(query: String): Flow<List<Job>> = jobDao.search(query)

    suspend fun getUnsyncedJobs(): List<Job> = jobDao.getUnsynced()
}
