package gautam.projects.event_hive.core.Navigation

import NotificationViewModelFactory
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging

import gautam.projects.event_hive.Presentation.screens.AddEventScreen
import gautam.projects.event_hive.core.Notifications.NotificationScreen
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.Presntation.ViewModel.SharedViewModel
import gautam.projects.event_hive.core.Notifications.NotificationViewModel
import gautam.projects.event_hive.Presntation.screens.*

import gautam.projects.event_hive.screens.AuthScreen

@Composable
fun NavigationControl(
    startEventId: String? = null, // For handling deep links from notifications
    navigateTo: String? = null    // For handling navigation from EventInfoActivity
) {
    val navController = rememberNavController()
    val user = Firebase.auth.currentUser

    val context=LocalContext.current.applicationContext
    val eventsViewModel: EventsViewModel=viewModel ()
    val sharedViewModel: SharedViewModel =viewModel()
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(
            context = context,
            nearbyEventsFlow = eventsViewModel.nearbyEvents
        )
    )

    // Handle navigation from EventInfoActivity to full EventInfoScreen
    LaunchedEffect(startEventId, navigateTo) {
        if (startEventId != null && navigateTo == "eventInfo") {
            navController.navigate(Routes.EventInfoScreen.createRoute(startEventId))
        } else {
            // Original logic for direct deep links from notifications
            startEventId?.let {
                navController.navigate(Routes.EventInfoScreen.createRoute(it))
            }
        }
    }

    LaunchedEffect(user) {
        if (user != null) {
            Firebase.messaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Firebase.firestore.collection("tokens")
                        .document(user.uid)
                        .set(mapOf("token" to token))
                }
            }
        }
    }

    val startDestination = remember {
        if (user != null) {
            Routes.SplashScreen.route
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
            MainScreen(navController)
        }

        composable(Routes.EditScreen.route) {
            EditProfileScreen(navController = navController)
        }

        composable(Routes.NotificationScreen.route) {
            NotificationScreen(
                navController = navController,
                viewModel = notificationViewModel
            )
        }

        composable(Routes.SplashScreen.route){
            SplashScreen(navController)
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

        composable(
            route=Routes.ConfirmScreen.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("quantity") { type = NavType.IntType},
                navArgument("amount") { type = NavType.IntType }
            )
        ){
                backStackEntry ->
            ConfirmScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId"),
                quantity = backStackEntry.arguments?.getInt("quantity")?: 0,
                amount = backStackEntry.arguments?.getInt("amount")?: 0
            )
        }
    }
}