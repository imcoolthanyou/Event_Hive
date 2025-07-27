package gautam.projects.event_hive.core.Navigation

sealed class Routes(val route: String) {

    object HomeScreen : Routes("home")
    object ProfileScreen : Routes("profile")
    object MapScreen : Routes("map")
    object SplashScreen : Routes("splash")
    object AuthScreen : Routes("auth")
    object BottomNavigation : Routes("main_app")
    object EditScreen : Routes("edit_profile")
    object SearchScreen : Routes("search")
    object NotificationScreen : Routes("notifications")
    object TicketScreen : Routes("ticket/{eventId}"){
        fun createRoute(eventId: String) = "ticket/$eventId"
    }

    // ✅ UPDATED: The route now defines an argument for eventId
    object EventInfoScreen : Routes("event_info/{eventId}") {
        // Helper function to create the full route with a specific ID
        fun createRoute(eventId: String) = "event_info/$eventId"
    }
    object AddEventScreen : Routes("add_event")

    object ConfirmScreen : Routes("confirm/{eventId}/{quantity}/{amount}") {
        fun createRoute(eventId: String, quantity: Int, amount: Int) = "confirm/$eventId/$quantity/$amount"
    }
    object SettingScreen:Routes("settings")




}