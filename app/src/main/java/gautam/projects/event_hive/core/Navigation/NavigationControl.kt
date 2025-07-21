package gautam.projects.event_hive.core.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import gautam.projects.event_hive.screens.AuthScreen

@Composable
fun NavigationControl(
    startEventId: String? = null
) {
    val navController = rememberNavController()
    LaunchedEffect(startEventId) {
        startEventId?.let {
            navController.navigate("eventInfo/$it")
        }
    }
    val context = LocalContext.current

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
        composable(
            route = "event_info/{eventId}",
            deepLinks = listOf(navDeepLink { uriPattern = "eventhive://event_info/{eventId}" })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            EventInfoScreen(navController,eventId = eventId)
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
            val vm = remember { NotificationViewModel(context) }
            NotificationScreen(navController, vm)
        }

        // ✅ VERIFIED: This is the correct way to define the route and its argument
        composable(
            route = Routes.EventInfoScreen.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            EventInfoScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId")
            )
        }

        composable(Routes.TicketScreen.route) {
            TicketScreen(navController = navController)
        }

        composable(Routes.AddEventScreen.route){
            AddEventScreen(navController = navController)
        }
    }
}
