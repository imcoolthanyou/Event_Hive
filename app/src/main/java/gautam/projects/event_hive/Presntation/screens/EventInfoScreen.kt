package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.core.Navigation.Routes
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInfoScreen(
    navController: NavController,
    eventId: String?,
    viewModel: EventsViewModel = viewModel()
) {
    // Fetch the event details when the screen is first composed
    LaunchedEffect(eventId) {
        if (eventId != null) {
            viewModel.getEventById(eventId)
        }
    }

    val event by viewModel.selectedEvent.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (event != null && event!!.ticketsAvailable > 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.bookTicket(eventId!!)
                        navController.navigate(Routes.TicketScreen.route)
                    },
                    icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                    text = { Text("Get Tickets (${event?.ticketsAvailable} left)") }
                )
            } else if (event != null) {
                ExtendedFloatingActionButton(
                    onClick = { /* Do nothing */ },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text("Sold Out")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        if (event == null) {
            // Show a loading indicator while the event data is being fetched
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentEvent = event!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(contentAlignment = Alignment.TopStart) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = currentEvent.imageUrls.firstOrNull()
                        ),
                        contentDescription = "Event Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(currentEvent.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    EventDetailRow(icon = Icons.Default.DateRange, text = currentEvent.date)
                    EventDetailRow(icon = Icons.Default.Schedule, text = currentEvent.time)
                    EventDetailRow(icon = Icons.Default.LocationOn, text = currentEvent.locationAddress)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("About this event:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(currentEvent.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Location Preview:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Mini Map Preview
                    MiniMap(latitude = currentEvent.latitude, longitude = currentEvent.longitude)
                    Spacer(modifier = Modifier.height(80.dp)) // Spacer for the FAB
                }
            }
        }
    }
}

@Composable
fun EventDetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MiniMap(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    val geoPoint = GeoPoint(latitude, longitude)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        AndroidView(
            factory = {
                MapView(it).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    isVerticalMapRepetitionEnabled = false
                    setMultiTouchControls(false)
                    controller.setZoom(16.0)
                    controller.setCenter(geoPoint)

                    val marker = Marker(this)
                    marker.position = geoPoint
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    overlays.add(marker)
                }
            }
        )
    }
}
