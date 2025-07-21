package gautam.projects.event_hive.Presntation.ViewModel

import android.location.Address
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import gautam.projects.event_hive.Data.Repository.EventsRepository
import gautam.projects.event_hive.Data.Repository.ProfileRepository
import gautam.projects.event_hive.Data.Repository.StorageRepository
import gautam.projects.event_hive.Data.model.SingleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EventsViewModel : ViewModel() {

    private val eventsRepository = EventsRepository()
    private val profileRepository = ProfileRepository()
    private val storageRepository = StorageRepository()

    val myEvents: StateFlow<List<SingleEvent>> = eventsRepository.myEvents
    val nearbyEvents: StateFlow<List<SingleEvent>> = eventsRepository.nearbyEvents
    val allPublicEvents: StateFlow<List<SingleEvent>> = eventsRepository.allPublicEvents

    private val _selectedEvent = MutableStateFlow<SingleEvent?>(null)
    val selectedEvent = _selectedEvent.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _isLoadingNearbyEvents = MutableStateFlow(false)
    val isLoadingNearbyEvents: StateFlow<Boolean> = _isLoadingNearbyEvents.asStateFlow()

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
            val userId = Firebase.auth.currentUser?.uid.orEmpty()
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
            // Handle success/failure if needed
        }
    }

    fun logOut() {
        Firebase.auth.signOut()
    }
}
