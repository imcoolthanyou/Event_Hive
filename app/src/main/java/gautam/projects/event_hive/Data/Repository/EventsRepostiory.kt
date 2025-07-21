// --- EventsRepository.kt ---
package gautam.projects.event_hive.Data.Repository

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import gautam.projects.event_hive.Data.model.SingleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class EventsRepository {

    private val db = Firebase.firestore
    private val eventsCollection = db.collection("events")

    private val _myEvents = MutableStateFlow<List<SingleEvent>>(emptyList())
    val myEvents = _myEvents.asStateFlow()

    private val _nearbyEvents = MutableStateFlow<List<SingleEvent>>(emptyList())
    val nearbyEvents = _nearbyEvents.asStateFlow()

    private val _allPublicEvents = MutableStateFlow<List<SingleEvent>>(emptyList())
    val allPublicEvents = _allPublicEvents.asStateFlow()

    init {
        listenForMyEventUpdates()
        listenForAllPublicEvents()
    }

    private fun listenForMyEventUpdates() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            _myEvents.value = emptyList()
            return
        }
        eventsCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("EventsRepository", "MyEvents listen failed.", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    _myEvents.value = it.toObjects(SingleEvent::class.java)
                }
            }
    }

    private fun listenForAllPublicEvents() {
        eventsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("EventsRepository", "AllEvents listen failed.", error)
                return@addSnapshotListener
            }
            snapshot?.let {
                _allPublicEvents.value = it.toObjects(SingleEvent::class.java)
            }
        }
    }

    suspend fun addEvent(event: SingleEvent) {
        try {
            eventsCollection.add(event).await()
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error adding event to Firestore", e)
        }
    }

    suspend fun getEventById(eventId: String): SingleEvent? {
        return try {
            eventsCollection.document(eventId).get().await().toObject(SingleEvent::class.java)
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error getting event details", e)
            null
        }
    }

    suspend fun bookTicketForEvent(eventId: String): Boolean {
        val eventRef = eventsCollection.document(eventId)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val currentTickets = snapshot.getLong("ticketsAvailable")?.toInt() ?: 0
                if (currentTickets > 0) {
                    transaction.update(eventRef, "ticketsAvailable", FieldValue.increment(-1))
                    null
                } else {
                    throw Exception("No tickets available.")
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error booking ticket", e)
            false
        }
    }

    suspend fun listenForNearbyEvents(centerLat: Double, centerLng: Double, radiusInKm: Double) {
        try {
            val allEvents = _allPublicEvents.value
            val nearby = allEvents.filter { event ->
                val distance = calculateDistance(
                    lat1 = centerLat,
                    lon1 = centerLng,
                    lat2 = event.latitude,
                    lon2 = event.longitude
                )
                distance <= radiusInKm
            }
            _nearbyEvents.value = nearby
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error filtering nearby events", e)
            _nearbyEvents.value = emptyList()
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}