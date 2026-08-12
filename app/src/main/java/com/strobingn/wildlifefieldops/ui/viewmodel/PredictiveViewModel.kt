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

data class SuggestedSlot(
    val job: Job,
    val score: Float,
    val reason: String,
    val suggestedHour: Int
)

@HiltViewModel
class PredictiveViewModel @Inject constructor(
    jobDao: JobDao
) : ViewModel() {

    val suggestions: StateFlow<List<SuggestedSlot>> = jobDao.getAll()
        .map { jobs ->
            jobs.filter {
                it.status != JobStatus.COMPLETED &&
                    it.status != JobStatus.PAID &&
                    it.status != JobStatus.CANCELLED
            }.map { job ->
                val (score, reason, hour) = scoreJob(job)
                SuggestedSlot(job, score, reason, hour)
            }.sortedByDescending { it.score }
                .take(12)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun scoreJob(job: Job): Triple<Float, String, Int> {
        var score = 50f
        val reasons = mutableListOf<String>()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        when (job.priority) {
            JobPriority.URGENT -> {
                score += 40f
                reasons += "Urgent priority"
            }
            JobPriority.HIGH -> {
                score += 25f
                reasons += "High priority"
            }
            JobPriority.MEDIUM -> score += 10f
            JobPriority.LOW -> score -= 5f
        }

        val isWaterfront = job.address.contains("river", true) ||
                job.address.contains("hudson", true) ||
                job.address.contains("bay", true) ||
                job.address.contains("creek", true) ||
                (job.latitude ?: 0.0) in 41.3..41.6

        if (isWaterfront) {
            if (hour in 6..9 || hour in 17..20) {
                score += 18f
                reasons += "Tide window favorable"
            } else {
                score -= 8f
                reasons += "Outside peak tide"
            }
        }

        if (job.scheduledDate != null) {
            val daysOut = ((job.scheduledDate - System.currentTimeMillis()) / 86_400_000L).toInt()
            if (daysOut in 0..2) {
                score += 15f
                reasons += "Due soon"
            }
        }

        if (job.estimatedValue > 500) {
            score += 8f
            reasons += "High value"
        }

        val suggestedHour = when {
            isWaterfront && hour < 12 -> 7
            isWaterfront -> 17
            hour < 12 -> 9
            else -> 14
        }

        val reason = if (reasons.isEmpty()) "Standard queue" else reasons.joinToString(" · ")
        return Triple(score.coerceIn(0f, 100f), reason, suggestedHour)
    }
}
