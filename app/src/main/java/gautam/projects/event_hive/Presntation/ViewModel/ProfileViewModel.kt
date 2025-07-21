package gautam.projects.event_hive.Presntation.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gautam.projects.event_hive.Data.Repository.ProfileRepository

import gautam.projects.event_hive.Data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    // The UI will collect this StateFlow to get the user's profile data.
    // This will automatically update the UI whenever the data changes in Firestore.
    val userProfile: StateFlow<UserProfile?> = repository.userProfile

    /**
     * Updates the user's discovery radius preference in Firestore.
     * @param radius The new radius value in kilometers.
     */
    fun updateDiscoveryRadius(radius: Int) {
        // Use viewModelScope to launch a coroutine for this background task.
        viewModelScope.launch {
            repository.updateDiscoveryRadius(radius)
        }
    }

    /**
     * Updates a specific notification preference for the user in Firestore.
     * @param preferenceName The name of the field in the UserProfile data class (e.g., "newEventsEnabled").
     * @param isEnabled The new boolean value for the preference.
     */
    fun updateNotificationPreference(preferenceName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationPreference(preferenceName, isEnabled)
        }
    }
}
