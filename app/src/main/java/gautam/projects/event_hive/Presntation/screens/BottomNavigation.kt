package gautam.projects.event_hive.Presntation.screens


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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import gautam.projects.event_hive.Data.model.NavItem
import gautam.projects.event_hive.Presentation.screens.HomeScreen
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.Presntation.ViewModel.NotificationViewModel
import gautam.projects.event_hive.core.Navigation.Routes

@Composable
fun BottomNavigation(navController: NavHostController) {
    val viewModel: EventsViewModel = viewModel()
    val myEvents by viewModel.myEvents.collectAsState()
    val notificationViewModel: NotificationViewModel=viewModel()

    val items = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("Search", Icons.Filled.Search, Icons.Outlined.Search),
        NavItem("Map", Icons.Filled.Place, Icons.Outlined.Place),
        NavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person),
        NavItem("Setting", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    var selectedItemTitle by remember { mutableStateOf("Home") }

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                items = items,
                selectedItemTitle = selectedItemTitle,
            ) { selectedItemTitle = it }
        },
        floatingActionButton = {
            if (selectedItemTitle == "Home") {
                FloatingActionButton(
                    // ✅ UPDATED: This now navigates to the new AddEventScreen
                    onClick = { navController.navigate(Routes.AddEventScreen.route) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItemTitle) {
                "Home" -> HomeScreen(
                    // ✅ UPDATED: The call to HomeScreen is now simplified
                    eventsViewModel = viewModel,
                    notificationViewModel = notificationViewModel,
                   onEventClick = {navController.navigate("EventDetailsScreen")}
                )
                "Search" -> SearchScreen(navController)
                "Map" -> MapScreen()
                "Setting" -> SettingsScreen(
                    navController = navController,
                    onLogOut = {
                        viewModel.logOut()
                        navController.navigate(Routes.AuthScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
                "Profile" -> ProfileScreen(navController)
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(
    items: List<NavItem>,
    selectedItemTitle: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = item.title == selectedItemTitle
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item.title) },
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
