import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import gautam.projects.event_hive.Data.model.SingleEvent
import gautam.projects.event_hive.core.Notifications.NotificationViewModel
import kotlinx.coroutines.flow.StateFlow

// ADD THIS FACTORY: To create the ViewModel instance with its parameters
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