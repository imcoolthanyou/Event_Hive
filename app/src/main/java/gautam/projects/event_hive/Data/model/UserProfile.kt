package gautam.projects.event_hive.Data.model

/**
 * Represents a user's profile and preferences stored in Firestore.
 * We use default values for all fields to make it easy for Firestore to
 * create new user documents from this class.
 */
data class UserProfile(
    val uid: String = "",
    val name: String? = "", // From Firebase Auth
    val email: String? = "", // From Firebase Auth
    val profilePictureUrl: String? = "",

    // User-configurable settings
    val discoveryRadius: Int = 5, // in kilometers, defaulting to 5km
    val newEventsEnabled: Boolean = true,
    val eventRemindersEnabled: Boolean = true
)
