// MainActivity.kt
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
import gautam.projects.event_hive.Presntation.ViewModel.NotificationViewModel
import gautam.projects.event_hive.core.Navigation.NavigationControl
import gautam.projects.event_hive.ui.theme.EventHiveTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }


        // ✅ UPDATED: Initialize Cloudinary using the new ApiKeys object
        val config = mapOf(
            "cloud_name" to ApiKeys.CLOUDINARY_CLOUD_NAME,
            "api_key" to ApiKeys.CLOUDINARY_API_KEY,
            "api_secret" to ApiKeys.CLOUDINARY_API_SECRET
        )
        MediaManager.init(this, config)

        // osmdroid configuration
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            EventHiveTheme {
                val eventId = intent?.getStringExtra("event_id")
                NavigationControl(startEventId = eventId)

            }
        }
    }
}

fun createEventDetailsPendingIntent(context: Context, eventId: String): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("event_id", eventId)
    }
    return PendingIntent.getActivity(
        context,
        eventId.hashCode(), // unique requestCode
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}