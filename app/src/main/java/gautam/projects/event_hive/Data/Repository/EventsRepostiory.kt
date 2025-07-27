package gautam.projects.event_hive.Data.Repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FieldValue
import gautam.projects.event_hive.Data.model.SingleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class EventsRepository {
    private val db = Firebase.firestore
    private val eventsCollection = db.collection("events")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // Scope for collecting flows

    private val _myEvents = MutableStateFlow<List<SingleEvent>>(emptyList())
    val myEvents: StateFlow<List<SingleEvent>> = _myEvents.asStateFlow()

    private val _nearbyEvents = MutableStateFlow<List<SingleEvent>>(emptyList())
    val nearbyEvents: StateFlow<List<SingleEvent>> = _nearbyEvents.asStateFlow()

    private val _allPublicEvents = MutableStateFlow<List<SingleEvent>>(emptyList())
    val allPublicEvents: StateFlow<List<SingleEvent>> = _allPublicEvents.asStateFlow()

    val savedEvents = MutableStateFlow<List<SingleEvent>>(emptyList())

    // Store the last parameters for filtering nearby events
    private var lastCenterLat: Double? = null
    private var lastCenterLng: Double? = null
    private var lastRadiusInKm: Double? = null

    init {
        listenForMyEventUpdates()
        listenForAllPublicEvents()
        // Start collecting allPublicEvents to update nearbyEvents reactively
        collectAllPublicEventsForNearby()
    }

    private fun collectAllPublicEventsForNearby() {
        scope.launch {
            // Use collectLatest to ensure we react to the latest value of allPublicEvents
            allPublicEvents.collectLatest { publicEvents ->
                // Re-filter nearby events if we have the necessary parameters
                val lat = lastCenterLat
                val lng = lastCenterLng
                val radius = lastRadiusInKm
                if (lat != null && lng != null && radius != null) {
                    Log.d("EventsRepository", "AllPublicEvents updated, re-filtering nearby events. Count: ${publicEvents.size}")
                    filterAndSetNearbyEvents(publicEvents, lat, lng, radius)
                } else {
                    Log.d("EventsRepository", "AllPublicEvents updated, but nearby filter params not set yet.")
                }
            }
        }
    }

    private fun listenForMyEventUpdates() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            _myEvents.value = emptyList()
            return
        }
        eventsCollection.whereEqualTo("userId", userId) // Assuming 'userId' field in Firestore doc
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
        // Assuming "public" is a boolean field or similar to identify public events
        // If all events in the 'events' collection are public, you can remove the where clause.
        // eventsCollection.whereEqualTo("public", true)
        eventsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("EventsRepository", "AllEvents listen failed.", error)
                return@addSnapshotListener
            }
            snapshot?.let {
                _allPublicEvents.value = it.toObjects(SingleEvent::class.java)
                Log.d("EventsRepository", "Updated allPublicEvents. New count: ${_allPublicEvents.value.size}")
            }
        }
    }

    suspend fun addEvent(event: SingleEvent) {
        try {
            val documentReference = eventsCollection.add(event).await()
            Log.d("EventsRepository", "Event added with ID: ${documentReference.id}")
            // The listener in listenForAllPublicEvents should pick up this change.
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
                    null // Indicate success
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

    // Make listenForNearbyEvents just store parameters and trigger the filter
    suspend fun listenForNearbyEvents(centerLat: Double, centerLng: Double, radiusInKm: Double) {
        Log.d("EventsRepository", "listenForNearbyEvents called with lat=$centerLat, lng=$centerLng, radius=$radiusInKm")
        // Store the parameters
        lastCenterLat = centerLat
        lastCenterLng = centerLng
        lastRadiusInKm = radiusInKm

        // Trigger filtering based on the current allPublicEvents value
        filterAndSetNearbyEvents(_allPublicEvents.value, centerLat, centerLng, radiusInKm)
    }

    // Extract the filtering logic into a separate function
    private fun filterAndSetNearbyEvents(eventsToFilter: List<SingleEvent>, centerLat: Double, centerLng: Double, radiusInKm: Double) {
        try {
            Log.d("EventsRepository", "filterAndSetNearbyEvents started with ${eventsToFilter.size} events.")
            val nearby = eventsToFilter.filter { event ->
                val distance = calculateDistance(
                    lat1 = centerLat,
                    lon1 = centerLng,
                    lat2 = event.latitude,
                    lon2 = event.longitude
                )
                distance <= radiusInKm
            }
            Log.d("EventsRepository", "filterAndSetNearbyEvents completed. Found ${nearby.size} nearby events.")
            _nearbyEvents.value = nearby
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error filtering nearby events", e)
            _nearbyEvents.value = emptyList()
        }
    }


    suspend fun toggleSavedEvent(event: SingleEvent) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        val savedRef = db.collection("users").document(userId) // Corrected path
            .collection("savedEvents").document(event.id)
        try {
            // Check if the event is already saved
            val snapshot = savedRef.get().await()
            if (snapshot.exists()) {
                // If exists, remove it (unsave)
                savedRef.delete().await()
                Log.d("Firestore", "Event unsaved for user.")
            } else {
                // If not exists, save it
                savedRef.set(event).await() // Save full event
                Log.d("Firestore", "Event saved for user.")
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error toggling saved event", e)
        }
    }

    suspend fun fetchSavedEvents() {
        val userId = Firebase.auth.currentUser?.uid ?: return
        try {
            // Corrected path: Assuming saved events are stored under user document
            val snapshot = db.collection("users").document(userId)
                .collection("savedEvents")
                .get().await()
            val events = snapshot.toObjects(SingleEvent::class.java)
            savedEvents.value = events
            Log.d("EventsRepository", "Fetched ${events.size} saved events.")
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error fetching saved events", e)
        }
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}