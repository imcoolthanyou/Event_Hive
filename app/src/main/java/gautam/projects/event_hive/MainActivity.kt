package gautam.projects.event_hive

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cloudinary.android.MediaManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.messaging.messaging
import com.razorpay.PaymentResultListener
import gautam.projects.event_hive.Presntation.screens.PaymentCallbackHelper
import gautam.projects.event_hive.core.Navigation.NavigationControl
import gautam.projects.event_hive.ui.theme.EventHiveTheme
import org.osmdroid.config.Configuration


class MainActivity : ComponentActivity(), PaymentResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        // ✅ Ensure token is always refreshed and saved on every app launch
        val user = Firebase.auth.currentUser
        if (user != null) {
            Firebase.messaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Firebase.firestore.collection("tokens")
                        .document(user.uid)
                        .set(mapOf("token" to token))
                        .addOnSuccessListener {
                            println("✅ Token saved to Firestore for ${user.uid}")
                        }
                        .addOnFailureListener {
                            println("❌ Failed to save token: ${it.message}")
                        }
                } else {
                    println("❌ Failed to get FCM token: ${task.exception?.message}")
                }
            }
        }


        // Initialize Cloudinary MediaManager
        try {
            MediaManager.init(
                this,
                mapOf(
                    "cloud_name" to ApiKeys.CLOUDINARY_CLOUD_NAME,
                    "api_key" to ApiKeys.CLOUDINARY_API_KEY,
                    "api_secret" to ApiKeys.CLOUDINARY_API_SECRET
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle Cloudinary initialization error if ApiKeys.kt is missing data
        }

        // OSMDroid configuration
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            EventHiveTheme {
                // Get the eventId from the notification intent
                val eventId = intent?.getStringExtra("eventId") ?: intent?.getStringExtra("event_id")
                val navigateTo = intent?.getStringExtra("navigateTo")
                NavigationControl(startEventId = eventId,
                    navigateTo = navigateTo)
            }
        }
    }

    override fun onPaymentSuccess(p0: String?) {
        PaymentCallbackHelper.onPaymentSuccess(p0?:"N/A")
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        val errorMessage = p1 ?: "Payment failed. Please try again."
        PaymentCallbackHelper.onPaymentError(errorMessage)
    }

}

/**
 * Creates a PendingIntent that opens the app directly to an event's detail screen.
 * This is used for notifications.
 */
fun createEventDetailsPendingIntent(context: Context, eventId: String): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("event_id", eventId)
    }
    // Use the eventId's hashcode as a unique request code for the PendingIntent
    return PendingIntent.getActivity(
        context,
        eventId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
