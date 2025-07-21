package gautam.projects.event_hive.Presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.valentinilk.shimmer.shimmer
import gautam.projects.event_hive.Data.model.SingleEvent
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.Presntation.ViewModel.NotificationViewModel
import gautam.projects.event_hive.Presntation.screens.EventCard


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEventClick: (SingleEvent) -> Unit,
    eventsViewModel: EventsViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val events by eventsViewModel.nearbyEvents.collectAsState()
    val isLoadingNearby by eventsViewModel.isLoadingNearbyEvents.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()

    val locationPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted.value = isGranted
        if (isGranted) {
            eventsViewModel.fetchNearbyEvents(28.6139, 77.2090)
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted.value) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            eventsViewModel.fetchNearbyEvents(28.6139, 77.2090)
        }
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoadingNearby)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Events") },
                actions = {
                    if (notifications.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .padding(end = 12.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                    }
                    IconButton(onClick = { notificationViewModel.showDummyNotification() }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Test Notification")
                    }
                }
            )
        },
        content = { paddingValues ->
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    eventsViewModel.fetchNearbyEvents(28.6139, 77.2090)
                },
                modifier = Modifier.padding(paddingValues)
            ) {
                when {
                    isLoadingNearby -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            repeat(5) {
                                ShimmerEventCard()
                            }
                        }
                    }

                    events.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No nearby events found.")
                        }
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(events) { event ->
                                EventCard(singleEvent = event, onCardClick = { onEventClick(event) })
                            }
                        }
                    }
                }
            }
        }
    )
}




@Composable
fun ShimmerEventCard() {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .size(width = 430.dp, height = 530.dp)
            .shimmer(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(16.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
