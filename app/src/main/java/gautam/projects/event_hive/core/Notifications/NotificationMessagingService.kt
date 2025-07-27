package gautam.projects.event_hive.core.Notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import gautam.projects.event_hive.Presntation.screens.EventInfoActivity
import gautam.projects.event_hive.R

class NotificationMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("tokens").document(userId)
            .set(mapOf("token" to token))
    }

        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun onMessageReceived(remoteMessage: RemoteMessage) {
            val eventId = remoteMessage.data["eventId"] ?: return

            val intent = Intent(this, EventInfoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("eventId", eventId)
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, "event_hive_notifications")
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(remoteMessage.notification?.title)
                .setContentText(remoteMessage.notification?.body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(this).notify(0, builder.build())
        }
    }
