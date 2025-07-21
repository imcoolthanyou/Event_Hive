package gautam.projects.event_hive.Presntation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel

import gautam.projects.event_hive.Presntation.ViewModel.ProfileViewModel
import gautam.projects.event_hive.core.helper.LocationAutocompleteTextField
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    var locationInputText by remember { mutableStateOf("") }

    // ✅ Get instances of the ViewModels
    val eventsViewModel: EventsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    // ✅ Collect the list of nearby events from the ViewModel
    val nearbyEvents by eventsViewModel.nearbyEvents.collectAsState()

    val mapView = rememberMapViewWithLifecycle()
    val mapController = mapView.controller

    // --- Function to trigger event fetching ---
    val fetchEventsForLocation = { lat: Double, lon: Double ->
        eventsViewModel.fetchNearbyEvents(lat, lon)
        mapController.animateTo(GeoPoint(lat, lon), 15.0, 1000L)
    }

    // --- Location Permission Handling ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        // Permission granted, get location and fetch events
                        fetchEventsForLocation(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(context, "Could not get current location.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // --- Initial Fetch on Screen Launch ---
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            update = { view ->
                // ✅ The map now updates based on the nearbyEvents list
                view.overlays.removeIf { it is Marker }
                nearbyEvents.forEach { event ->
                    val eventGeoPoint = GeoPoint(event.latitude, event.longitude)
                    val eventMarker = Marker(view)
                    eventMarker.position = eventGeoPoint
                    eventMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    eventMarker.title = event.title
                    eventMarker.snippet = event.locationAddress
                    view.overlays.add(eventMarker)
                }
                view.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- Autocomplete Search Bar ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LocationAutocompleteTextField(
                value = locationInputText,
                onValueChange = { locationInputText = it },
                onLocationSelected = { address ->
                    locationInputText = address.getAddressLine(0)
                    // When a location is searched, fetch events for that new location
                    fetchEventsForLocation(address.latitude, address.longitude)
                }
            )
        }

        // --- "My Location" Floating Action Button ---
        FloatingActionButton(
            onClick = {
                // Re-request permission and fetch for current location
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center on my location")
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(23.2599, 77.4126)) // Default center
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    return mapView
}
