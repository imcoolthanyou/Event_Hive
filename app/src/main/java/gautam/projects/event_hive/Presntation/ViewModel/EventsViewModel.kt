package gautam.projects.event_hive.Presntation.ViewModel

import android.location.Address
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import gautam.projects.event_hive.Data.Repository.EventsRepository
import gautam.projects.event_hive.Data.Repository.ProfileRepository
import gautam.projects.event_hive.Data.Repository.StorageRepository
import gautam.projects.event_hive.Data.model.SingleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class EventsViewModel : ViewModel() {

    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val eventsRepository = EventsRepository()
    private val profileRepository = ProfileRepository()
    private val storageRepository = StorageRepository()
    private val functions = FirebaseFunctions.getInstance()

    val myEvents: StateFlow<List<SingleEvent>> = eventsRepository.myEvents
    val nearbyEvents: StateFlow<List<SingleEvent>> = eventsRepository.nearbyEvents
    val allPublicEvents: StateFlow<List<SingleEvent>> = eventsRepository.allPublicEvents

    private val _upcomingEvents = MutableStateFlow<List<SingleEvent>>(emptyList())
    val upcomingEvents: StateFlow<List<SingleEvent>> = _upcomingEvents.asStateFlow()

    private val _selectedEvent = MutableStateFlow<SingleEvent?>(null)
    val selectedEvent: StateFlow<SingleEvent?> = _selectedEvent.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _isLoadingNearbyEvents = MutableStateFlow(false)
    val isLoadingNearbyEvents: StateFlow<Boolean> = _isLoadingNearbyEvents.asStateFlow()

    private val _razorpayOrderResponse = MutableStateFlow<Map<String, Any>?>(null)
    val razorpayOrderResponse = _razorpayOrderResponse.asStateFlow()

    // Search-related state flows
    private val _searchResults = MutableStateFlow<List<SingleEvent>>(emptyList())
    val searchResults: StateFlow<List<SingleEvent>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Debounce mechanism for real-time search
    private var searchJob: kotlinx.coroutines.Job? = null

    val savedEvent= eventsRepository.savedEvents.asStateFlow()

    init {
        viewModelScope.launch {
            allPublicEvents.collect { events ->
                _upcomingEvents.value = events
                    .filter { isFutureOrToday(it.date) }
                    .sortedBy { parseDate(it.date) }
            }
        }
    }

    fun createEvent(
        imgUris: List<Uri>,
        title: String,
        location: Address,
        date: String,
        time: String,
        description: String,
        totalTickets: Int
    ) {
        viewModelScope.launch {
            _isCreating.value = true
            val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val imageUrls = imgUris.mapNotNull { uri -> storageRepository.uploadImage(uri) }

            val newEvent = SingleEvent(
                userId = userId,
                title = title,
                locationAddress = location.getAddressLine(0) ?: "",
                latitude = location.latitude,
                longitude = location.longitude,
                date = date,
                time = time,
                description = description,
                imageUrls = imageUrls,
                totalTickets = totalTickets,
                ticketsAvailable = totalTickets,
                createdBy = userId
            )

            eventsRepository.addEvent(newEvent)
            _isCreating.value = false
        }
    }

    fun fetchNearbyEvents(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoadingNearbyEvents.value = true
            val userProfile = profileRepository.userProfile.first()
            val radiusInKm = userProfile?.discoveryRadius?.toDouble() ?: 5.0

            eventsRepository.listenForNearbyEvents(
                centerLat = latitude,
                centerLng = longitude,
                radiusInKm = radiusInKm
            )
            _isLoadingNearbyEvents.value = false
        }
    }

    fun getEventById(eventId: String) {
        viewModelScope.launch {
            _selectedEvent.value = eventsRepository.getEventById(eventId)
        }
    }

    fun bookTicket(eventId: String) {
        viewModelScope.launch {
            val success = eventsRepository.bookTicketForEvent(eventId)
            if (success) {
                getEventById(eventId)
            }
        }
    }

    fun createRazorpayOrder(amountInRupees: Int) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "amount" to amountInRupees * 100, // convert to paise
                    "currency" to "INR"
                )

                val result = functions
                    .getHttpsCallable("createRazorpayOrder")
                    .call(data)
                    .await()

                _razorpayOrderResponse.value = result.data as? Map<String, Any>
            } catch (e: Exception) {
                Log.e("EventsViewModel", "Error calling createRazorpayOrder", e)
                _razorpayOrderResponse.value = null
            }
        }
    }

    fun clearOrderResponse() {
        _razorpayOrderResponse.value = null
    }

    fun logOut() {
        FirebaseAuth.getInstance().signOut()
    }

    // Real-time search function with debouncing
    fun searchEventsRealTime(query: String) {
        // Cancel previous search job
        searchJob?.cancel()

        _searchQuery.value = query

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        // Start new search job with debouncing
        searchJob = viewModelScope.launch {
            // Wait for 300ms before starting search (debouncing)
            kotlinx.coroutines.delay(300)
            performSearch(query)
        }
    }

    // Immediate search function (for when user presses search or selects suggestion)
    fun searchEvents(query: String) {
        searchJob?.cancel()
        _searchQuery.value = query

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            performSearch(query)
        }
    }

    // Core search implementation
    private suspend fun performSearch(query: String) {
        _isSearching.value = true

        try {
            val db = Firebase.firestore
            val searchTerms = query.lowercase().trim().split(" ").filter { it.isNotBlank() }

            // Get all events from Firestore
            val eventsSnapshot = db.collection("events").get().await()
            val allEvents = eventsSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SingleEvent::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("EventsViewModel", "Error parsing event: ${doc.id}", e)
                    null
                }
            }

            // Filter events based on comprehensive search criteria
            val filteredEvents = allEvents.filter { event ->
                searchTerms.any { term ->
                    // Search in title (highest priority)
                    event.title.lowercase().contains(term) ||
                            // Search in description
                            event.description.lowercase().contains(term) ||
                            // Search in location address
                            event.locationAddress.lowercase().contains(term) ||

                            // Search by city/area (extract from address)
                            extractCityFromAddress(event.locationAddress).lowercase().contains(term)
                }
            }.filter {
                // Only show future events
                isFutureOrToday(it.date)
            }.sortedWith(
                compareByDescending<SingleEvent> { event ->
                    // Prioritize exact matches in title
                    when {
                        event.title.lowercase().contains(query.lowercase()) -> 3
                        event.locationAddress.lowercase().contains(query.lowercase()) -> 2
                        event.description.lowercase().contains(query.lowercase()) -> 1
                        else -> 0
                    }
                }.thenBy { parseDate(it.date) }
            )

            _searchResults.value = filteredEvents

        } catch (e: Exception) {
            Log.e("EventsViewModel", "Error searching events", e)
            _searchResults.value = emptyList()
        } finally {
            _isSearching.value = false
        }
    }

    // Helper function to extract city from address
    private fun extractCityFromAddress(address: String): String {
        // Split by comma and take relevant parts
        val parts = address.split(",")
        return if (parts.size >= 2) {
            parts[parts.size - 2].trim() // Usually city is second last part
        } else {
            address
        }
    }

    // Clear search results
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchQuery.value = ""
    }



    private fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    private fun isFutureOrToday(dateString: String): Boolean {
        val eventDate = parseDate(dateString) ?: return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return !eventDate.before(today.time)
    }
    fun toggleSave(event: SingleEvent){
        viewModelScope.launch {
           eventsRepository.toggleSavedEvent(event)
        }
    }
    fun fetchSavedEvents() {
        viewModelScope.launch {
            eventsRepository.fetchSavedEvents()
        }
    }
}