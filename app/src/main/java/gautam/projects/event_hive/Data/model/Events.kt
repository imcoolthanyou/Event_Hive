// SingleEvent.kt
package gautam.projects.event_hive.Data.model

import com.google.firebase.firestore.DocumentId

data class SingleEvent(
    // This annotation tells Firestore to automatically populate this field
    // with the document's unique ID. This is crucial for navigation.
    @DocumentId val id: String = "",

    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val locationAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrls: List<String> = emptyList(),
    val totalTickets: Int = 0,
    val ticketsAvailable: Int = 0,
    val createdBy: String = ""
)
