package com.strobingn.wildlife.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.strobingn.wildlife.data.local.JobDao
import com.strobingn.wildlife.data.model.DefaultServiceTypes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serviceTypeDataStore by preferencesDataStore(name = "service_types")

/**
 * Built-in + user-defined service types for jobs.
 */
@Singleton
class ServiceTypeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jobDao: JobDao
) {
    private val dataStore = context.serviceTypeDataStore

    private val customKey = stringSetPreferencesKey("custom_service_types")

    val customTypes: Flow<List<String>> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            prefs[customKey]
                .orEmpty()
                .map { DefaultServiceTypes.normalize(it) }
                .filter { it.isNotBlank() }
                .sortedBy { it.lowercase() }
        }

    /** Built-ins first, then custom (deduped, case-insensitive). */
    val allTypes: Flow<List<String>> = customTypes.map { custom ->
        val seen = linkedSetOf<String>()
        val out = mutableListOf<String>()
        (DefaultServiceTypes.all + custom).forEach { label ->
            val n = DefaultServiceTypes.normalize(label)
            val key = n.lowercase()
            if (n.isNotBlank() && key !in seen) {
                seen += key
                out += n
            }
        }
        out
    }

    suspend fun addCustomType(label: String): Boolean {
        val n = DefaultServiceTypes.normalize(label)
        if (n.isBlank()) return false
        // Don't store duplicates of built-ins
        if (DefaultServiceTypes.all.any { it.equals(n, ignoreCase = true) }) return true
        dataStore.edit { prefs ->
            val current = prefs[customKey].orEmpty().toMutableSet()
            current.removeAll { it.equals(n, ignoreCase = true) }
            current += n
            prefs[customKey] = current
        }
        return true
    }

    /**
     * Remove a custom service type from the catalog.
     * @param reassignJobsTo if set, jobs using this type are updated to the new label.
     * @return number of jobs that referenced this type
     */
    suspend fun removeCustomType(
        label: String,
        reassignJobsTo: String? = DefaultServiceTypes.all.first()
    ): Int {
        val n = DefaultServiceTypes.normalize(label)
        if (DefaultServiceTypes.all.any { it.equals(n, ignoreCase = true) }) {
            // Built-ins cannot be deleted
            return 0
        }
        val usage = jobDao.countByServiceType(n)
        // Also match case-insensitive variants via exact Room match first
        val reassigned = if (reassignJobsTo != null && usage > 0) {
            val target = DefaultServiceTypes.display(reassignJobsTo)
            jobDao.reassignServiceType(n, target)
        } else {
            0
        }
        dataStore.edit { prefs ->
            val current = prefs[customKey].orEmpty().toMutableSet()
            current.removeAll { it.equals(n, ignoreCase = true) }
            prefs[customKey] = current
        }
        return maxOf(usage, reassigned)
    }

    suspend fun countJobsUsing(label: String): Int {
        val n = DefaultServiceTypes.normalize(label)
        return jobDao.countByServiceType(n)
    }

    fun isBuiltIn(label: String): Boolean =
        DefaultServiceTypes.all.any { it.equals(DefaultServiceTypes.normalize(label), ignoreCase = true) }
}
