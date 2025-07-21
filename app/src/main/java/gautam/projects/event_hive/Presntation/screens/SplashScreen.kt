// SplashScreen.kt
package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import gautam.projects.event_hive.R // Make sure your R file is imported
import gautam.projects.event_hive.core.Navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // This loads your splash_animation.json file from the res/raw directory
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))

    // This state tracks the animation's progress
    val progress by animateLottieCompositionAsState(composition)

    // This block runs when the animation's progress changes
    LaunchedEffect(progress) {
        // When the animation is 100% complete (progress == 1f), navigate away.
        if (progress == 1f) {
            val destination = if (Firebase.auth.currentUser != null) {
                Routes.BottomNavigation.route // Already logged in
            } else {
                Routes.AuthScreen.route // Needs to log in
            }

            navController.navigate(destination) {
                popUpTo(Routes.SplashScreen.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent), // Set the background color you want
        contentAlignment = Alignment.Center
    ) {
        // This is the composable that actually displays the animation
        LottieAnimation(
            composition = composition,
            progress = { progress },
        )
    }
}