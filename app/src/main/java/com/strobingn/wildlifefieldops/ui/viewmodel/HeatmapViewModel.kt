package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.local.TrapLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HeatPoint(val lat: Double, val lng: Double, val intensity: Double)

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    jobDao: JobDao,
    trapLogDao: TrapLogDao
) : ViewModel() {

    val heatPoints = combine(jobDao.getAll(), trapLogDao.getAll()) { jobs, traps ->
        val buckets = mutableMapOf<String, HeatPoint>()
        fun key(lat: Double, lng: Double) = "${(lat * 200).toInt()}_${(lng * 200).toInt()}"

        jobs.filter { it.latitude != null && it.longitude != null }.forEach { j ->
            val k = key(j.latitude!!, j.longitude!!)
            val existing = buckets[k]
            buckets[k] = HeatPoint(
                j.latitude!!, j.longitude!!,
                (existing?.intensity ?: 0.0) + 1.4
            )
        }
        traps.filter { it.latitude != null && it.longitude != null }.forEach { t ->
            val k = key(t.latitude!!, t.longitude!!)
            val existing = buckets[k]
            buckets[k] = HeatPoint(
                t.latitude!!, t.longitude!!,
                (existing?.intensity ?: 0.0) + 0.9
            )
        }
        buckets.values.sortedByDescending { it.intensity }.take(80)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
