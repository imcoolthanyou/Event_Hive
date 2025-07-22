import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    navController: NavController,
    eventId: String?,
    viewModel: EventsViewModel = viewModel()
) {
    // The ticket screen also observes the selected event
    val event by viewModel.selectedEvent.collectAsState()
    var ticketCount by remember { mutableIntStateOf(1) }
    var isBooking by remember { mutableStateOf(false) }

    // This makes sure the event details are loaded if the user deep-links here
    LaunchedEffect(eventId) {
        if (eventId != null && event?.id != eventId) {
            viewModel.getEventById(eventId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Your Ticket") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (event == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(event!!.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(event!!.locationAddress, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Text("${event!!.date} at ${event!!.time}", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

                Spacer(modifier = Modifier.height(32.dp))

                // --- Placeholder for Payment Details ---
                Text("PAYMENT METHOD", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("In a real app, you would integrate a payment SDK like Stripe or Razorpay here.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom

                Button(
                    onClick = {
                        isBooking = true
                        // In a real app, this would be called after successful payment
                        viewModel.bookTicket(eventId!!)
                        // Here you could show a success dialog or navigate to a final confirmation page
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isBooking
                ) {
                    if (isBooking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Confirm & Book Ticket")
                    }
                }
            }
        }
    }
}

