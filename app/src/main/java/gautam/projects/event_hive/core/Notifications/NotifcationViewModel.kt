// File: gautam/projects/event_hive/core/Notifications/NotificationViewModel.kt
package gautam.projects.event_hive.core.Notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import gautam.projects.event_hive.Data.model.SingleEvent
import gautam.projects.event_hive.Presntation.screens.EventInfoActivity // Note: Typo in package name
import gautam.projects.event_hive.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "event_hive_notifications"
private const val TAG = "NotificationViewModel" // Tag for logging

class NotificationViewModel(
    private val context: Context,
    nearbyEventsFlow: StateFlow<List<SingleEvent>>
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<SingleEvent>>(emptyList())
    val notifications: StateFlow<List<SingleEvent>> = _notifications.asStateFlow()

    init {
        createNotificationChannel()
        saveFCMTokenToFirestore()
        collectNearbyEvents(nearbyEventsFlow)
    }

    private fun collectNearbyEvents(nearbyEventsFlow: StateFlow<List<SingleEvent>>) {
        // **** LOG CURRENT USER ID EARLY ****
        val currentUserId = Firebase.auth.currentUser?.uid
        Log.d(TAG, "Current User ID for filtering: $currentUserId")
        viewModelScope.launch {
            nearbyEventsFlow.collect { eventList ->
                Log.d(TAG, "Received ${eventList.size} nearby events from the data flow.")

                // **** LOG DETAILS OF RECEIVED EVENTS ****
                eventList.forEach { event ->
                    Log.d(TAG, "Event ID: ${event.id}, Title: '${event.title}', CreatedBy: '${event.createdBy}'")
                }

                // Filter 1: Exclude events created by the current user
                val filtered = eventList.filter { it.createdBy != currentUserId }
                Log.d(TAG, "After filtering by 'createdBy != currentUserId': ${filtered.size} events remain.")
                filtered.forEach { event ->
                    Log.d(TAG, "Filtered Event ID: ${event.id}, Title: '${event.title}', CreatedBy: '${event.createdBy}'")
                }

                // Filter 2: Exclude events already notified about (based on ID)
                val newOnes = filtered.filter { newEvent ->
                    val isNew = _notifications.value.none { existingEvent -> existingEvent.id == newEvent.id }
                    if (!isNew) {
                        Log.d(TAG, "Event ID '${newEvent.id}' is already in notifications list, skipping.")
                    }
                    isNew
                }
                Log.d(TAG, "After filtering by 'not already notified': ${newOnes.size} events are truly new.")

                if (newOnes.isNotEmpty()) {
                    Log.d(TAG, "Found ${newOnes.size} new events to notify about. Updating UI list.")
                    // Update the list observed by the UI
                    _notifications.value = _notifications.value + newOnes
                    Log.d(TAG, "UI List updated. New size: ${_notifications.value.size}") // *** ADDED ***
                    // Send system notification for each new event
                    newOnes.forEach { sendSystemNotification(it) }
                } else {
                    Log.d(TAG, "No *new* events to notify about after all filters.")
                    // **** ADD THIS LINE TO CHECK CURRENT NOTIFICATIONS LIST ****
                    Log.d(TAG, "Current notifications list size: ${_notifications.value.size}")
                    _notifications.value.forEach { existingEvent ->
                        Log.d(TAG, "Already notified about: ID='${existingEvent.id}', Title='${existingEvent.title}'")
                    }
                }
            }
        }
    }

    private fun sendSystemNotification(event: SingleEvent) {
        // Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted. You might want to request it or skip notification.
            Log.w(TAG, "Notification permission not granted. Skipping system notification for event: ${event.title}")
            return
        }

        val intent = Intent(context, EventInfoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("eventId", event.id)
        }

        // Use FLAG_IMMUTABLE for PendingIntent (required for Android 12+)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // Make sure this drawable exists
            .setContentTitle("New Nearby Event!")
            .setContentText("Check out: ${event.title}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Automatically removes the notification when tapped

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            // Using current time millis ensures uniqueness
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
        Log.d(TAG, "Sent system notification for event: ${event.title}")
    }

    // **** NEW DIAGNOSTIC FUNCTION ****
    /**
     * Call this from a button in your UI to test if the NotificationScreen list works.
     * This adds a fake event directly to the list your screen is observing.
     */
    fun addTestNotificationForUI() {
        val testEvent = SingleEvent(
            id = "test_${System.currentTimeMillis()}",
            title = "This is a Test Event",
            locationAddress = "Check if this appears in the list",
            createdBy = "test_user",
            // Initialize other required fields to avoid potential crashes
            userId = "",
            latitude = 0.0,
            longitude = 0.0,
            date = "",
            time = "",
            description = "",
            imageUrls = emptyList(),
            totalTickets = 0,
            ticketsAvailable = 0

        )
        _notifications.value = _notifications.value + testEvent
        Log.d(TAG, "Manually added a test event to the UI list. New list size: ${_notifications.value.size}") // *** ADDED ***
    }

    // --- Other functions are unchanged ---

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
            Log.d(TAG, "Notification channel created/updated.")
        }
    }

    private fun saveFCMTokenToFirestore() {
        val userId = Firebase.auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Firebase.firestore.collection("tokens").document(userId)
                .set(mapOf("token" to token))
                .addOnSuccessListener {
                    Log.d(TAG, "FCM Token saved successfully for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to save FCM Token for user: $userId", e)
                }
        }
    }

    fun onNewToken(token: String) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("tokens").document(userId)
            .set(mapOf("token" to token))
            .addOnSuccessListener {
                Log.d(TAG, "FCM Token updated successfully for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to update FCM Token for user: $userId", e)
            }
    }
}

// Factory class (assuming it exists and is used correctly)
class NotificationViewModelFactory(
    private val context: Context,
    private val nearbyEventsFlow: StateFlow<List<SingleEvent>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(context, nearbyEventsFlow) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}