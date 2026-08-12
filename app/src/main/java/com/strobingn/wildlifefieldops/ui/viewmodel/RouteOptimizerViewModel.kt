package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.data.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
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

    /**
     * v2 optimizer:
     * - Nearest-neighbor base
     * - Priority weighting (URGENT / HIGH first)
     * - Simple Hudson tide factor (prefer waterfront jobs near high tide windows)
     * - Simulated traffic multiplier (rush hours cost more)
     */
    fun optimize(jobs: List<Job>): List<Job> {
        if (jobs.size < 2) return jobs

        val remaining = jobs.toMutableList()
        val optimized = mutableListOf<Job>()

        // Start with highest priority job that has location
        val start = remaining.minByOrNull { priorityScore(it) } ?: remaining.removeFirst()
        optimized += start
        remaining.remove(start)

        while (remaining.isNotEmpty()) {
            val current = optimized.last()
            val next = remaining.minByOrNull { candidate ->
                val dist = distanceMeters(current, candidate)
                val prio = priorityScore(candidate)
                val tide = tidePenalty(candidate)
                val traffic = trafficMultiplier()
                (dist * traffic) + prio + tide
            } ?: break
            optimized += next
            remaining.remove(next)
        }
        return optimized
    }

    private fun priorityScore(job: Job): Float = when (job.priority) {
        JobPriority.URGENT -> 0f
        JobPriority.HIGH -> 800f
        JobPriority.MEDIUM -> 2500f
        JobPriority.LOW -> 5000f
    }

    /** Crude Hudson River tide preference – waterfront jobs preferred near high tide */
    private fun tidePenalty(job: Job): Float {
        val isWaterfront = job.address.contains("river", true) ||
                job.address.contains("hudson", true) ||
                job.address.contains("bay", true) ||
                job.address.contains("creek", true) ||
                (job.latitude ?: 0.0) in 41.3..41.6
        if (!isWaterfront) return 0f

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Prefer ~06-09 and 17-20 local for high-ish tides in lower Hudson
        return if (hour in 6..9 || hour in 17..20) -1200f else 1800f
    }

    private fun trafficMultiplier(): Float {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 7..9 || hour in 16..18 -> 1.55f   // rush
            hour in 11..13 -> 1.15f
            else -> 1.0f
        }
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
