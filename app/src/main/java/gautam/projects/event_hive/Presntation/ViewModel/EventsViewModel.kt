package gautam.projects.event_hive.Presntation.ViewModel

import android.location.Address
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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
}