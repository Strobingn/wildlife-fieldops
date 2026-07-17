package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RouteOptimizerViewModel @Inject constructor(
    jobDao: JobDao
) : ViewModel() {

    val routableJobs: StateFlow<List<Job>> = jobDao.getAll()
        .map { jobs ->
            jobs.filter { job ->
                job.latitude != null &&
                    job.longitude != null &&
                    job.status != JobStatus.COMPLETED &&
                    job.status != JobStatus.PAID
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun optimize(jobs: List<Job>): List<Job> {
        if (jobs.size < 3) return jobs
        val remaining = jobs.toMutableList()
        val optimized = mutableListOf(remaining.removeAt(0))
        while (remaining.isNotEmpty()) {
            val current = optimized.last()
            val next = remaining.minByOrNull { candidate -> distanceMeters(current, candidate) } ?: break
            optimized += next
            remaining.remove(next)
        }
        return optimized
    }

    private fun distanceMeters(a: Job, b: Job): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude ?: 0.0,
            a.longitude ?: 0.0,
            b.latitude ?: 0.0,
            b.longitude ?: 0.0,
            result
        )
        return result[0]
    }
}
