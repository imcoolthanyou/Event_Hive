package gautam.projects.event_hive.Data.model



// Represents the logged-in user
data class User(
    val name: String,
    val email: String,
    val profilePictureUrl: String // We'll use this with Coil to load the image
)

// A simplified event model for the list items
data class ProfileEvent(
    val imageUrl: String,
    val title: String,
    val dateTime: String,
    val location: String
)
