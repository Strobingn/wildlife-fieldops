package com.strobingn.wildlifefieldops

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.strobingn.wildlifefieldops.navigation.Screen
import com.strobingn.wildlifefieldops.ui.components.BrandMark
import com.strobingn.wildlifefieldops.ui.screens.*
import com.strobingn.wildlifefieldops.ui.theme.WildlifeFieldOpsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (permission, granted) ->
            android.util.Log.d("Permissions", "$permission: $granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WildlifeFieldOpsTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(800)
                        requestLaunchPermissions()
                    }
                    WildlifeFieldOpsNavHost()
                }
            }
        }
    }

    private fun requestLaunchPermissions() {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WildlifeFieldOpsNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomNav = currentRoute in Screen.bottomNavItems.map { it.route }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showBottomNav,
        drawerContent = {
            AppDrawer(
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    ModernBottomBar(currentRoute ?: Screen.Dashboard.route) { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        ) { padding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(padding),
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit
) {
    NavHost(navController, Screen.Dashboard.route, modifier) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToJobs = { navController.navigate(Screen.JobList.createRoute()) },
                onNavigateToInspections = { navController.navigate(Screen.InspectionList.route) },
                onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                onNavigateToJobDetail = { navController.navigate(Screen.JobDetail.createRoute(it)) },
                onNavigateToJobForm = { navController.navigate(Screen.JobForm.createRoute()) },
                onNavigateToCustomers = { navController.navigate(Screen.CustomerList.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAI = { navController.navigate(Screen.AIAssistant.route) },
                onOpenDrawer = onOpenDrawer
            )
        }

        composable(
            route = Screen.JobList.route,
            arguments = listOf(
                navArgument("status") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("serviceType") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { entry ->
            val status = entry.arguments?.getString("status")
                ?.let { runCatching { JobStatus.valueOf(it) }.getOrNull() }
            val serviceType = entry.arguments?.getString("serviceType")
            JobListScreen(
                onNavigateToJobDetail = { navController.navigate(Screen.JobDetail.createRoute(it)) },
                onNavigateToJobForm = { navController.navigate(Screen.JobForm.createRoute()) },
                onBack = { navController.popBackStack() },
                showBack = false,
                initialStatus = status,
                initialServiceType = serviceType
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() },
                onOpenJobs = { status, serviceType ->
                    navController.navigate(Screen.JobList.createRoute(status?.name, serviceType))
                }
            )
        }

        composable(Screen.InspectionList.route) {
            InspectionListScreen(
                onNavigateToInspectionDetail = { navController.navigate(Screen.InspectionDetail.createRoute(it)) },
                onNavigateToInspectionForm = { navController.navigate(Screen.InspectionForm.createRoute()) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Schedule.route) {
            ScheduleScreen(
                onNavigateToJobDetail = { navController.navigate(Screen.JobDetail.createRoute(it)) },
                onNavigateToJobForm = { navController.navigate(Screen.JobForm.createRoute()) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.GPS.route) {
            GPSScreen(
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CustomerList.route) {
            CustomerListScreen(
                onNavigateToCustomerForm = { navController.navigate(Screen.CustomerForm.createRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                onBack = { navController.popBackStack() },
                onNavigateToJobDetail = { navController.navigate(Screen.JobDetail.createRoute(it)) }
            )
        }
        composable(Screen.PhotoGallery.route) {
            PhotoGalleryScreen(onBack = { navController.popBackStack() }, viewModel = hiltViewModel())
        }
        composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.AIAssistant.route) { AIAssistantScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Expense.route) { ExpenseScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Inventory.route) { InventoryScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.RouteOptimizer.route) { RouteOptimizerScreen(onBack = { navController.popBackStack() }) }

        composable(
            Screen.JobDetail.route,
            listOf(navArgument("jobId") { type = NavType.StringType })
        ) { entry ->
            val jobId = entry.arguments?.getString("jobId").orEmpty()
            JobDetailScreen(
                jobId = jobId,
                onNavigateToEdit = { navController.navigate(Screen.JobForm.createRoute(it)) },
                onNavigateToInvoice = { navController.navigate(Screen.Invoice.createRoute(jobId)) },
                onNavigateToEstimate = { navController.navigate(Screen.Estimate.createRoute(jobId)) },
                onNavigateToInspectionForm = { navController.navigate(Screen.InspectionForm.createRoute()) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.JobForm.route,
            listOf(navArgument("jobId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("jobId")?.takeUnless { it == "new" || it.isBlank() }
            JobFormScreen(jobId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Screen.CustomerForm.route,
            listOf(navArgument("customerId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { entry ->
            CustomerFormScreen(entry.arguments?.getString("customerId")) { navController.popBackStack() }
        }
        composable(
            Screen.InspectionDetail.route,
            listOf(navArgument("inspectionId") { type = NavType.StringType })
        ) { entry ->
            InspectionFormScreen(entry.arguments?.getString("inspectionId")) { navController.popBackStack() }
        }
        composable(
            Screen.InspectionForm.route,
            listOf(navArgument("inspectionId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { entry ->
            InspectionFormScreen(entry.arguments?.getString("inspectionId")) { navController.popBackStack() }
        }
        composable(
            Screen.Invoice.route,
            listOf(navArgument("jobId") { type = NavType.StringType })
        ) { entry ->
            InvoiceScreen(entry.arguments?.getString("jobId").orEmpty()) { navController.popBackStack() }
        }
        composable(
            Screen.Estimate.route,
            listOf(navArgument("jobId") { type = NavType.StringType })
        ) { entry ->
            EstimateScreen(entry.arguments?.getString("jobId").orEmpty()) { navController.popBackStack() }
        }
    }
}

@Composable
private fun ModernBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar {
        Screen.bottomNavItems.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route || (screen == Screen.JobList && currentRoute.startsWith("jobs")),
                onClick = { onNavigate(if (screen == Screen.JobList) Screen.JobList.createRoute() else screen.route) },
                icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                label = { Text(screen.title) }
            )
        }
    }
}

@Composable
private fun AppDrawer(onNavigate: (String) -> Unit, onClose: () -> Unit) {
    ModalDrawerSheet {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark(modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Text("Wildlife FieldOps", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider()
        Screen.drawerItems.forEach { screen ->
            NavigationDrawerItem(
                label = { Text(screen.title) },
                selected = false,
                onClick = { onNavigate(screen.route) },
                icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClose, modifier = Modifier.padding(16.dp)) { Text("Close") }
    }
}
