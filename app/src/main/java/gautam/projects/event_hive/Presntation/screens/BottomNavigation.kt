package gautam.projects.event_hive.Presntation.screens

import NotificationViewModelFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import gautam.projects.event_hive.Data.model.NavItem
import gautam.projects.event_hive.core.Notifications.NotificationScreen
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.Presntation.ViewModel.SharedViewModel
import gautam.projects.event_hive.core.Navigation.Routes
import gautam.projects.event_hive.core.Notifications.NotificationViewModel



// This can be your main screen in the navigation graph after login.
@Composable
fun MainScreen(
    // This is the NavController for your app's overall navigation (e.g., to go back to the login screen)
    appNavController: NavHostController
) {
    // This new NavController is specifically for the screens inside the bottom navigation
    val bottomBarNavController = rememberNavController()

    // --- ViewModel Setup from previous answer ---
    // You create your ViewModels here, at the highest shared level.
    val context = LocalContext.current.applicationContext
    val eventsViewModel: EventsViewModel = viewModel()
    val sharedViewModel: SharedViewModel = viewModel()
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(
            context = context,
            nearbyEventsFlow = eventsViewModel.nearbyEvents
        )
    )
    // --- End ViewModel Setup ---

    val items = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, Routes.HomeScreen.route),
        NavItem("Search", Icons.Filled.Search, Icons.Outlined.Search, Routes.SearchScreen.route),
        NavItem("Map", Icons.Filled.Place, Icons.Outlined.Place, Routes.MapScreen.route),
        NavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person, Routes.ProfileScreen.route),
        NavItem(title ="Settings", Icons.Filled.Settings, Icons.Outlined.Settings, Routes.SettingScreen.route)

    )

    val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                items = items,
                currentDestination = currentDestination,
                onItemSelected = { route ->
                    bottomBarNavController.navigate(route) {

                        popUpTo(bottomBarNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        },
        floatingActionButton = {
            // Show FAB only when on the Home screen
            if (currentDestination?.route == Routes.HomeScreen.route) {
                FloatingActionButton(
                    onClick = { appNavController.navigate(Routes.AddEventScreen.route) },
                    shape = CircleShape,
                    containerColor = Color.Black,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Event",
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        // This NavHost replaces your 'when' block
        NavHost(
            navController = bottomBarNavController,
            startDestination = Routes.HomeScreen.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HomeScreen.route) {
                HomeScreen(
                    navController = appNavController, // Use the main controller for navigating away
                    viewModel = eventsViewModel,
                    sharedViewModel = sharedViewModel,
                    onViewOnMapClick = { bottomBarNavController.navigate(Routes.MapScreen.route) },
                    onSearchClick = { bottomBarNavController.navigate(Routes.SearchScreen.route) },
                    onProfileClick = { bottomBarNavController.navigate(Routes.ProfileScreen.route) },
                    onMyEventsClick = { bottomBarNavController.navigate(Routes.ProfileScreen.route) }
                )
            }
            composable(Routes.SearchScreen.route) {
                SearchScreen(navController = appNavController, eventsViewModel = eventsViewModel)
            }
            composable(Routes.MapScreen.route) {
                MapScreen(sharedViewModel = sharedViewModel, eventsViewModel = eventsViewModel)
            }
            composable(Routes.ProfileScreen.route) {
                ProfileScreen(
                    navController = appNavController,
                    viewModel = eventsViewModel,
                    onLogOut = {
                        appNavController.navigate(Routes.AuthScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            // These are now top-level destinations navigated to by the appNavController
            composable(Routes.NotificationScreen.route) {
                NotificationScreen(navController = appNavController, viewModel = notificationViewModel)
            }
            composable(Routes.SettingScreen.route) {
                SettingsScreen(
                    navController = appNavController,
                    onLogOut = {
                        appNavController.navigate(Routes.AuthScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

        }
    }
}


@Composable
fun AppBottomNavigationBar(
    items: List<NavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            // The item is selected if its route is part of the current destination's hierarchy
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item.route) }, // Use the route for navigation
                icon = {
                    Column(
                        modifier = if (isSelected) {
                            Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color.Black)
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        } else {
                            Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = if (isSelected) Color.White else Color.Gray
                        )
                        if (isSelected) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// You'll need to add 'route' to your NavItem data class
data class NavItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String // Add this
)