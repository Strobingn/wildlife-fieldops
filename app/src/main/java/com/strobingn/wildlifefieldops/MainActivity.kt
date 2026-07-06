package com.strobingn.wildlifefieldops

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.strobingn.wildlifefieldops.navigation.Screen
import com.strobingn.wildlifefieldops.ui.screens.*
import com.strobingn.wildlifefieldops.ui.theme.WildlifeFieldOpsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        permissions.entries.forEach { (permission, granted) ->
            android.util.Log.d("Permissions", "$permission: $granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request essential permissions
        requestEssentialPermissions()

        setContent {
            WildlifeFieldOpsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WildlifeFieldOpsApp()
                }
            }
        }
    }

    private fun requestEssentialPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WildlifeFieldOpsApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom nav only on main screens
    val showBottomNav = currentRoute in Screen.bottomNavItems.map { it.route }
    var showDrawer by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
        gesturesEnabled = showBottomNav,
        drawerContent = {
            if (showBottomNav) {
                AppDrawer(
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        showDrawer = false
                    },
                    onClose = { showDrawer = false }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    BottomNavigationBar(
                        currentRoute = currentRoute ?: Screen.Dashboard.route,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onMenuClick = { showDrawer = true }
                    )
                }
            }
        ) { padding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // Main screens
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToJobs = { navController.navigate(Screen.JobList.route) },
                onNavigateToInspections = { navController.navigate(Screen.InspectionList.route) },
                onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                onNavigateToJobDetail = { id -> navController.navigate(Screen.JobDetail.createRoute(id)) },
                onNavigateToJobForm = { navController.navigate(Screen.JobForm.createRoute()) },
                onNavigateToCustomers = { navController.navigate(Screen.CustomerList.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAI = { navController.navigate(Screen.AIAssistant.route) }
            )
        }

        composable(Screen.JobList.route) {
            JobListScreen(
                onNavigateToJobDetail = { id -> navController.navigate(Screen.JobDetail.createRoute(id)) },
                onNavigateToJobForm = { navController.navigate(Screen.JobForm.createRoute()) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            JobDetailScreen(
                jobId = jobId,
                onNavigateToEdit = { navController.navigate(Screen.JobForm.createRoute(jobId)) },
                onNavigateToInvoice = { navController.navigate(Screen.Invoice.createRoute(jobId)) },
                onNavigateToEstimate = { navController.navigate(Screen.Estimate.createRoute(jobId)) },
                onNavigateToInspectionForm = { navController.navigate(Screen.InspectionForm.createRoute()) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JobForm.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            JobFormScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.InspectionList.route) {
            InspectionListScreen(
                onNavigateToInspectionDetail = { id -> navController.navigate(Screen.InspectionDetail.createRoute(id)) },
                onNavigateToInspectionForm = { navController.navigate(Screen.InspectionForm.createRoute()) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.InspectionDetail.route,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType })
        ) {
            // Placeholder - navigates back
            LaunchedEffect(Unit) { navController.popBackStack() }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF22c55e))
            }
        }

        composable(
            route = Screen.InspectionForm.route,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            InspectionFormScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Schedule.route) {
            ScheduleScreen(
                onNavigateToJobDetail = { id -> navController.navigate(Screen.JobDetail.createRoute(id)) },
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

        // Drawer screens
        composable(Screen.CustomerList.route) {
            CustomerListScreen(
                onNavigateToCustomerForm = { id ->
                    navController.navigate(Screen.CustomerForm.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CustomerForm.route,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId")
            CustomerFormScreen(
                customerId = customerId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onBack = { navController.popBackStack() },
                onNavigateToJobDetail = { id -> navController.navigate(Screen.JobDetail.createRoute(id)) }
            )
        }

        composable(
            route = Screen.Invoice.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            InvoiceScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PhotoGallery.route) {
            PhotoGalleryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AIAssistant.route) {
            AIAssistantScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Expense.route) {
            ExpenseScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Inventory.route) {
            InventoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RouteOptimizer.route) {
            RouteOptimizerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Estimate.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            EstimateScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color(0xFF0f0f1a),
        tonalElevation = 0.dp
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = androidx.compose.ui.graphics.Color(0xFFa0a0b0))
        }

        Screen.bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            val color = if (selected) androidx.compose.ui.graphics.Color(0xFF22c55e) else androidx.compose.ui.graphics.Color(0xFF6b6b80)

            NavigationBarItem(
                icon = {
                    screen.icon?.let {
                        Icon(it, contentDescription = screen.title, tint = color)
                    }
                },
                label = {
                    Text(
                        screen.title,
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = selected,
                onClick = { onNavigate(screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.ui.graphics.Color(0xFF22c55e),
                    selectedTextColor = androidx.compose.ui.graphics.Color(0xFF22c55e),
                    indicatorColor = androidx.compose.ui.graphics.Color(0xFF22c55e).copy(alpha = 0.15f),
                    unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF6b6b80),
                    unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF6b6b80)
                )
            )
        }
    }
}

@Composable
private fun AppDrawer(
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = androidx.compose.ui.graphics.Color(0xFF13131f),
        drawerContentColor = androidx.compose.ui.graphics.Color(0xFFf0f0f5)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Drawer Header
        Text(
            "Wildlife FieldOps",
            style = MaterialTheme.typography.headlineSmall,
            color = androidx.compose.ui.graphics.Color(0xFF22c55e),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            "Field Operations Center",
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color(0xFF6b6b80),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = androidx.compose.ui.graphics.Color(0xFF2a2a3f)
        )

        // Drawer Items
        Screen.drawerItems.forEach { screen ->
            NavigationDrawerItem(
                icon = {
                    screen.icon?.let {
                        Icon(it, contentDescription = screen.title, tint = androidx.compose.ui.graphics.Color(0xFFa0a0b0))
                    }
                },
                label = {
                    Text(
                        screen.title,
                        color = androidx.compose.ui.graphics.Color(0xFFf0f0f5)
                    )
                },
                selected = false,
                onClick = { onNavigate(screen.route) },
                modifier = Modifier.padding(horizontal = 8.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unselectedTextColor = androidx.compose.ui.graphics.Color(0xFFf0f0f5),
                    unselectedIconColor = androidx.compose.ui.graphics.Color(0xFFa0a0b0)
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = androidx.compose.ui.graphics.Color(0xFF2a2a3f)
        )

        Text(
            "v1.1 - Rockstar Edition",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color(0xFF6b6b80),
            modifier = Modifier.padding(16.dp)
        )
    }
}
