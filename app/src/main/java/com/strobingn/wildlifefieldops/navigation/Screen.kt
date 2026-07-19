package com.strobingn.wildlifefieldops.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object JobList : Screen("jobs", "Jobs", Icons.Default.Work)
    object InspectionList : Screen("inspections", "Inspections", Icons.Default.Search)
    object Schedule : Screen("schedule", "Schedule", Icons.Default.CalendarMonth)
    object GPS : Screen("gps", "GPS", Icons.Default.LocationOn)

    object JobDetail : Screen("job_detail/{jobId}", "Job Detail") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    object JobForm : Screen("job_form/{jobId}", "Job Form") {
        fun createRoute(jobId: String? = null) = "job_form/${jobId ?: "new"}"
    }

    object CustomerList : Screen("customers", "Customers", Icons.Default.People)
    object CustomerForm : Screen("customer_form?customerId={customerId}", "Customer Form") {
        fun createRoute(customerId: String? = null) =
            if (customerId != null) "customer_form?customerId=$customerId" else "customer_form"
    }

    object InspectionDetail : Screen("inspection_detail/{inspectionId}", "Inspection Detail") {
        fun createRoute(inspectionId: String) = "inspection_detail/$inspectionId"
    }
    object InspectionForm : Screen(
        "inspection_form?inspectionId={inspectionId}&jobId={jobId}",
        "Inspection Form"
    ) {
        fun createRoute(inspectionId: String? = null, jobId: String? = null): String {
            val params = buildList {
                if (!inspectionId.isNullOrBlank()) add("inspectionId=$inspectionId")
                if (!jobId.isNullOrBlank()) add("jobId=$jobId")
            }
            return if (params.isEmpty()) "inspection_form" else "inspection_form?${params.joinToString("&")}"
        }
    }

    object Map : Screen("map", "Property Map", Icons.Default.Map)
    object Invoice : Screen("invoice/{jobId}", "Invoice") {
        fun createRoute(jobId: String) = "invoice/$jobId"
    }
    object PhotoGallery : Screen("photos", "Photo Gallery", Icons.Default.PhotoCamera)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Diagnostics : Screen("diagnostics", "Diagnostics", Icons.Default.BugReport)
    object AIAssistant : Screen("ai_assistant", "AI Assistant", Icons.Default.Psychology)
    object SpeciesId : Screen("species_id", "Species ID", Icons.Default.Pets)
    object VoiceJob : Screen("voice_job", "Voice Job", Icons.Default.RecordVoiceOver)
    object ARMeasure : Screen("ar_measure", "AR Measure", Icons.Default.Straighten)
    object Expense : Screen("expenses", "Expenses", Icons.Default.Receipt)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Inventory)
    object RouteOptimizer : Screen("routes", "Routes", Icons.Default.Route)
    object Estimate : Screen("estimate/{jobId}", "Estimate") {
        fun createRoute(jobId: String) = "estimate/$jobId"
    }
    object SyncQueue : Screen("sync_queue", "Sync Queue", Icons.Default.CloudSync)
    object InspectionScheduler : Screen("inspection_scheduler", "Inspection Scheduler", Icons.Default.EventNote)
    object Contract : Screen("contract/{jobId}", "Contract") {
        fun createRoute(jobId: String) = "contract/$jobId"
    }
    object VoiceDictation : Screen("voice_dictation", "Voice Dictation", Icons.Default.Mic)
    object MlKitCamera : Screen("mlkit_camera", "AI Camera", Icons.Default.CameraAlt)

    /** Base route is arg-free so drawer navigation works (no `{sessionId}` literal). */
    object FieldCapture : Screen("field_capture", "Field Capture", Icons.Default.AddAPhoto) {
        const val ROUTE_WITH_SESSION = "field_capture?sessionId={sessionId}"
        fun createRoute(sessionId: String? = null): String =
            if (sessionId.isNullOrBlank()) route
            else "field_capture?sessionId=$sessionId"
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, JobList, InspectionList, Schedule, GPS)
        val drawerItems = listOf(
            CustomerList,
            Map,
            PhotoGallery,
            Expense,
            Inventory,
            RouteOptimizer,
            FieldCapture,
            InspectionScheduler,
            SyncQueue,
            VoiceJob,
            VoiceDictation,
            SpeciesId,
            MlKitCamera,
            ARMeasure,
            AIAssistant,
            Settings
        )
    }
}
