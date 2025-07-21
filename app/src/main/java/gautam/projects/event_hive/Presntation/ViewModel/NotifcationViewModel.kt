package gautam.projects.event_hive.Presntation.ViewModel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import gautam.projects.event_hive.Data.model.SingleEvent
import gautam.projects.event_hive.Presntation.screens.EventInfoActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "event_hive_notifications"

class NotificationViewModel(private val context: Context) : ViewModel() {

    private val eventsViewModel = EventsViewModel()

    private val _notifications = MutableStateFlow<List<SingleEvent>>(emptyList())
    val notifications: StateFlow<List<SingleEvent>> = _notifications.asStateFlow()

    init {
        createNotificationChannel()
        val currentUser = Firebase.auth.currentUser?.uid

        viewModelScope.launch {
            eventsViewModel.nearbyEvents.collect { list ->
                val filtered = list.filter { it.userId != currentUser }
                val newOnes = filtered.filter { it !in _notifications.value }

                if (newOnes.isNotEmpty()) {
                    _notifications.value = _notifications.value + newOnes

                    newOnes.forEach { event ->
                        sendSystemNotification(event)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Nearby Events"
            val description = "Notifications for events near you"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val manager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendSystemNotification(event: SingleEvent) {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (event.userId == currentUserId) return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, EventInfoActivity::class.java).apply {
            putExtra("eventId", event.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Event Nearby: ${event.title}")
            .setContentText(event.locationAddress)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(context).notify(event.id.hashCode(), builder.build())
    }

    /** For testing a dummy notification manually **/
    fun showDummyNotification() {
        val dummyEvent = SingleEvent(
            id = "dummy123",
            userId = "someone_else",
            title = "Test Event",
            description = "Test description",
            date = "2025-08-01",
            time = "12:00 PM",
            locationAddress = "Connaught Place, Delhi",
            latitude = 0.0,
            longitude = 0.0
        )
        sendSystemNotification(dummyEvent)
    }
}
