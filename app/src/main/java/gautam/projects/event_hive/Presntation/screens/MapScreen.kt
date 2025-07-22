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
import gautam.projects.event_hive.Presntation.ViewModel.SharedViewModel
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    eventsViewModel: EventsViewModel = viewModel(),

    ) {
    val  sharedViewModel: SharedViewModel = viewModel()
    val context = LocalContext.current
    val nearbyEvents by eventsViewModel.nearbyEvents.collectAsState()
    val initialTargetState by sharedViewModel.initialMapTarget.collectAsState()

    val mapView = rememberMapViewWithLifecycle()

    // --- This effect handles the intelligent zoom from other screens ---
    LaunchedEffect(initialTargetState) {
        initialTargetState?.let { state ->
            if (state.zoomToFitEvents.isNotEmpty()) {
                val points = state.zoomToFitEvents.map { GeoPoint(it.latitude, it.longitude) }
                val boundingBox = BoundingBox.fromGeoPoints(points)
                mapView.post {
                    mapView.zoomToBoundingBox(boundingBox, true, 100) // 100px padding
                }
            } else if (state.singleTarget != null) {
                mapView.controller.animateTo(state.singleTarget, 15.0, 1000L)
            }
            sharedViewModel.clearInitialMapTarget() // Reset the target
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        // Only fetch and zoom if no initial target was set
                        if (initialTargetState == null) {
                            eventsViewModel.fetchNearbyEvents(location.latitude, location.longitude)
                            mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude), 14.0, 1000L)
                        }
                    } else {
                        Toast.makeText(context, "Could not get location.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            update = { view ->
                view.overlays.removeIf { it is Marker }
                nearbyEvents.forEach { event ->
                    val eventMarker = Marker(view)
                    eventMarker.position = GeoPoint(event.latitude, event.longitude)
                    eventMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    eventMarker.title = event.title
                    eventMarker.snippet = event.locationAddress
                    view.overlays.add(eventMarker)
                }
                view.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        FloatingActionButton(
            onClick = {
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

// rememberMapViewWithLifecycle() composable remains the same


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
