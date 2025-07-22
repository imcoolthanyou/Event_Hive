package gautam.projects.event_hive.core.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import gautam.projects.event_hive.Presentation.screens.AddEventScreen
import gautam.projects.event_hive.Presentation.screens.NotificationScreen
import gautam.projects.event_hive.Presntation.ViewModel.NotificationViewModel
import gautam.projects.event_hive.Presntation.screens.*
import gautam.projects.event_hive.core.helper.NotificationViewModelFactory
import gautam.projects.event_hive.screens.AuthScreen

@Composable
fun NavigationControl(
    startEventId: String? = null // For handling deep links from notifications
) {
    val navController = rememberNavController()

    // This effect will run once if a startEventId is provided (e.g., from a notification)
    // and navigate to the correct event details screen.
    LaunchedEffect(startEventId) {
        startEventId?.let {
            // ✅ FIX: Use the type-safe helper function to create the route
            navController.navigate(Routes.EventInfoScreen.createRoute(it))
        }
    }

    val startDestination = remember {
        if (Firebase.auth.currentUser != null) {
            Routes.BottomNavigation.route
        } else {
            Routes.AuthScreen.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.AuthScreen.route) {
            AuthScreen(
                onSignInSuccess = {
                    navController.navigate(Routes.BottomNavigation.route) {
                        popUpTo(Routes.AuthScreen.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.BottomNavigation.route) {
            BottomNavigation(navController = navController)
        }

        composable(Routes.EditScreen.route) {
            EditProfileScreen(navController = navController)
        }

        composable(Routes.SearchScreen.route) {
            SearchScreen(navController = navController)
        }

        composable(Routes.NotificationScreen.route) {
            val context = LocalContext.current
            val vm: NotificationViewModel = viewModel(
                factory = NotificationViewModelFactory(context)
            )
            NotificationScreen(navController, vm)
        }


        composable(
            route = Routes.EventInfoScreen.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "eventhive://event_info/{eventId}" })
        ) { backStackEntry ->
            EventInfoScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId")
            )
        }

        composable(Routes.TicketScreen.route,
            arguments = listOf(navArgument("eventId"){type = NavType.StringType})) {backStackEntry ->

            TicketScreen(navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId"))
        }

        composable(Routes.AddEventScreen.route) {
            AddEventScreen(navController = navController)
        }
    }
}
