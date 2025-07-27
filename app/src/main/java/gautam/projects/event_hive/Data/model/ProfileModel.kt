package gautam.projects.event_hive.Data.model

import android.net.Uri


// Represents the logged-in user
data class User(
    val name: String?="",
    val email: String?="",
    val profilePictureUrl: Uri? = null // We'll use this with Coil to load the image
)

// A simplified event model for the list items
data class ProfileEvent(
    val imageUrl: List<String> = emptyList(),
    val title: String,
    val dateTime: String,
    val location: String,
    val id: String
)
