package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import gautam.projects.event_hive.Data.model.SingleEvent
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.Presntation.ViewModel.SharedViewModel
import gautam.projects.event_hive.R
import gautam.projects.event_hive.core.Navigation.Routes
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: EventsViewModel = viewModel(),
    sharedViewModel: SharedViewModel = viewModel(),
    onViewOnMapClick: () -> Unit
) {
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val nearbyEvents by viewModel.nearbyEvents.collectAsState()

    var selectedDate by remember { mutableStateOf(Calendar.getInstance().time) }

    val currentUserId =viewModel.userId


    val filteredEvents = remember(selectedDate, upcomingEvents, currentUserId) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val selectedDateStr = sdf.format(selectedDate)
        upcomingEvents.filter {
            it.date == selectedDateStr && it.createdBy != currentUserId
        }
    }

    val nearbyPreviewEvent = remember(filteredEvents, nearbyEvents, currentUserId) {
        filteredEvents.firstOrNull { event ->
            event.createdBy!= currentUserId && nearbyEvents.any { nearby -> nearby.id == event.id }
        }
    }

    Scaffold(
        topBar = { HomeTopBar(navController) },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { CalendarView(selectedDate) { newDate -> selectedDate = newDate } }
            item {
                SectionHeader(
                    title = "Upcoming Events",
                    actionText = "My Events",
                    onActionClick = { /* TODO: Navigate to My Events screen */ }
                )
            }
            item {
                if (filteredEvents.isEmpty()) {
                    Text(
                        "No events scheduled for this day.",
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredEvents, key = { it.id }) { event ->
                            UpcomingEventCard(event = event, onEventClick = {
                                navController.navigate(Routes.EventInfoScreen.createRoute(event.id))
                            })
                        }
                    }
                }
            }
            item {
                SectionHeader(
                    title = "Nearby Events",
                    actionText = "View on Map",
                    onActionClick = {
                        // Set the target for the MapScreen before navigating
                        sharedViewModel.setInitialMapTarget(
                            target = nearbyPreviewEvent?.let { GeoPoint(it.latitude, it.longitude) },
                            zoomToFitAll = if (nearbyPreviewEvent == null) nearbyEvents else emptyList()
                        )
                        onViewOnMapClick() // This switches the tab in BottomNavigation
                    }
                )
            }
            item { NearbyEventsPreview(nearbyEvent = nearbyPreviewEvent) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(navController: NavController) {
    TopAppBar(
        title = { Text("Event Hive", fontWeight = FontWeight.Bold, color = Color.Black) },
        navigationIcon = {
            IconButton(onClick = { navController.navigate("Profile") }) {
                Icon(Icons.Default.Person, contentDescription = "Profile")
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate(Routes.SearchScreen.route) }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { navController.navigate(Routes.NotificationScreen.route) }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun CalendarView(selectedDate: Date, onDateSelected: (Date) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.time = Date() // Start from today
    val dates = remember {
        List(30) {
            calendar.time.also { calendar.add(Calendar.DAY_OF_YEAR, 1) }
        }
    }
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
    val selectedDayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dates) { date ->
                val isSelected = selectedDayFormat.format(date) == selectedDayFormat.format(selectedDate)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onDateSelected(date) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = dayFormat.format(date).uppercase(),
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = dateFormat.format(date),
                        color = if (isSelected) Color.White else Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        TextButton(onClick = onActionClick) {
            Text(actionText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UpcomingEventCard(event: SingleEvent, onEventClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(250.dp)
            .clickable(onClick = onEventClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(
                    model = event.imageUrls.firstOrNull(),
                    error = painterResource(id = R.drawable.placeholder_event)
                ),
                contentDescription = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.locationAddress,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NearbyEventsPreview(nearbyEvent: SingleEvent?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    isVerticalMapRepetitionEnabled = false
                    setMultiTouchControls(false)
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                if (nearbyEvent != null) {
                    val geoPoint = GeoPoint(nearbyEvent.latitude, nearbyEvent.longitude)
                    mapView.controller.setZoom(15.0)
                    mapView.controller.setCenter(geoPoint)
                    val marker = Marker(mapView)
                    marker.position = geoPoint
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )
        if (nearbyEvent == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("No nearby events for this day.", color = Color.Gray)
            }
        }
    }
}