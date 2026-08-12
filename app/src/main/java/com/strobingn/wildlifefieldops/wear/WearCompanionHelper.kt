package com.strobingn.wildlifefieldops.wear

/**
 * Phone-side helper for future Wear OS companion.
 * Ready for Wearable Data Layer / MessageClient integration.
 * Full :wear module can be added later without breaking phone builds.
 */
object WearCompanionHelper {

    const val PATH_JOB_STATUS = "/fieldops/job_status"
    const val PATH_QUICK_NOTE = "/fieldops/quick_note"
    const val PATH_TRAP_CHECK = "/fieldops/trap_check"

    fun isWearAvailable(): Boolean = false // real check via Wearable.getCapabilityClient later

    fun buildJobStatusPayload(
        jobId: String,
        title: String,
        status: String,
        priority: String
    ): Map<String, String> = mapOf(
        "jobId" to jobId,
        "title" to title,
        "status" to status,
        "priority" to priority,
        "ts" to System.currentTimeMillis().toString()
    )
}
