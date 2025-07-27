
package gautam.projects.event_hive.Presntation.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultListener
import gautam.projects.event_hive.ApiKeys
import gautam.projects.event_hive.Data.model.SingleEvent
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel // Note: Typo in package name
import gautam.projects.event_hive.core.Navigation.Routes
import org.json.JSONObject

private const val TAG = "TicketScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    navController: NavHostController,
    eventId: String?, // This can be null if navigation argument is missing
    viewModel: EventsViewModel = viewModel()
) {
    val context = LocalContext.current as Activity
    val event by viewModel.selectedEvent.collectAsState()
    val razorpayOrderResponse by viewModel.razorpayOrderResponse.collectAsState()
    var isBooking by remember { mutableStateOf(false) }
    var ticketQuantity by remember { mutableStateOf(1) }
    val ticketPrice = 100 // You can make this dynamic based on the event model later

    // --- ADD LOGGING ---
    LaunchedEffect(Unit) {
        Log.d(TAG, "TicketScreen Composable entered. Received eventId: $eventId")
    }
    // --- END ADD LOGGING ---

    // Load event details when the screen is first composed or eventId changes
    // --- IMPROVED LaunchedEffect ---
    LaunchedEffect(eventId) {
        Log.d(TAG, "LaunchedEffect triggered. eventId: $eventId")
        if (eventId == null) {
            // Handle the case where eventId is missing from navigation arguments
            Log.e(TAG, "eventId is null. Cannot load event. Popping back stack.")
            Toast.makeText(context, "Error: Event ID not found.", Toast.LENGTH_SHORT).show()
            navController.popBackStack() // Navigate back if eventId is invalid
            return@LaunchedEffect // Exit the effect early
        }

        // Only fetch if the currently selected event is not the one we want
        // or if there is no selected event yet.
        if (event?.id != eventId) {
            Log.d(TAG, "Calling viewModel.getEventById for eventId: $eventId")
            viewModel.getEventById(eventId)
        } else {
            Log.d(TAG, "Event already loaded or matches eventId.")
        }
    }
    // --- END IMPROVED LaunchedEffect ---

    // This effect observes the response from our cloud function.
    // When the orderId is received, it launches the Razorpay checkout.
    LaunchedEffect(razorpayOrderResponse) {
        razorpayOrderResponse?.let { response ->
            val orderId = response["orderId"] as? String
            if (orderId != null) {
                // Ensure eventId is not null here as well, though the check above should prevent it
                val safeEventId = eventId
                if (safeEventId != null) {
                    startRazorpayCheckout(
                        activity = context,
                        orderId = orderId,
                        amountInRupees = ticketPrice * ticketQuantity,
                        eventTitle = event?.title ?: "Event Ticket",
                        onPaymentSuccess = { paymentId ->
                            isBooking = false
                            // Book the tickets in Firestore after successful payment
                            repeat(ticketQuantity) {
                                viewModel.bookTicket(safeEventId)
                            }
                            // Navigate to the confirmation screen
                            navController.navigate(
                                Routes.ConfirmScreen.createRoute(
                                    eventId = safeEventId,
                                    quantity = ticketQuantity,
                                    amount = ticketPrice * ticketQuantity
                                )
                            ) {
                                // Clear this screen from the back stack
                                popUpTo(Routes.TicketScreen.createRoute(safeEventId)) { inclusive = true }
                            }
                        },
                        onPaymentError = { error ->
                            isBooking = false
                            Toast.makeText(context, "Payment Failed: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Log.e(TAG, "Cannot proceed to payment: eventId is null.")
                    isBooking = false
                }
            }
            viewModel.clearOrderResponse()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Tickets", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (eventId == null || event == null) { // Show loading if eventId is null or event hasn't loaded yet
            LoadingScreen()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                EventHeaderCard(event = event!!)
                Spacer(modifier = Modifier.height(16.dp))
                TicketDetailsCard(
                    event = event!!,
                    ticketPrice = ticketPrice,
                    ticketQuantity = ticketQuantity,
                    onQuantityChange = { newQuantity ->
                        if (newQuantity in 1..minOf(5, event!!.ticketsAvailable)) {
                            ticketQuantity = newQuantity
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PaymentSummaryCard(
                    ticketPrice = ticketPrice,
                    quantity = ticketQuantity,
                    totalAmount = ticketPrice * ticketQuantity
                )
                Spacer(modifier = Modifier.height(24.dp))
                BookingButton(
                    isBooking = isBooking,
                    isAvailable = event!!.ticketsAvailable >= ticketQuantity,
                    totalAmount = ticketPrice * ticketQuantity,
                    quantity = ticketQuantity,
                    onClick = {
                        if (event!!.ticketsAvailable >= ticketQuantity) {
                            isBooking = true
                            viewModel.createRazorpayOrder(amountInRupees = ticketPrice * ticketQuantity)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading event details...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EventHeaderCard(event: SingleEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(12.dp))
                EventDetailRow(icon = Icons.Default.LocationOn, text = event.locationAddress)
                Spacer(modifier = Modifier.height(8.dp))
                EventDetailRow(icon = Icons.Default.DateRange, text = "${event.date} at ${event.time}")
                Spacer(modifier = Modifier.height(8.dp))
                EventDetailRow(icon = Icons.Outlined.ConfirmationNumber, text = "${event.ticketsAvailable} tickets available")
            }
        }
    }
}



@Composable
private fun TicketDetailsCard(
    event: SingleEvent,
    ticketPrice: Int,
    ticketQuantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Ticket Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Regular Ticket", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("₹$ticketPrice per ticket", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    IconButton(onClick = { onQuantityChange(ticketQuantity - 1) }, enabled = ticketQuantity > 1, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(20.dp))
                    }
                    Text(ticketQuantity.toString(), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onQuantityChange(ticketQuantity + 1) }, enabled = ticketQuantity < minOf(5, event.ticketsAvailable), modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase quantity", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentSummaryCard(ticketPrice: Int, quantity: Int, totalAmount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Payment Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            SummaryRow("Ticket Price", "₹$ticketPrice")
            SummaryRow("Quantity", quantity.toString())
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SummaryRow("Total Amount", "₹$totalAmount", isTotal = true)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BookingButton(
    isBooking: Boolean,
    isAvailable: Boolean,
    totalAmount: Int,
    quantity: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        enabled = !isBooking && isAvailable,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        AnimatedContent(
            targetState = when {
                isBooking -> "loading"
                !isAvailable -> "unavailable"
                else -> "available"
            },
            transitionSpec = { fadeIn() with fadeOut() }
        ) { state ->
            when (state) {
                "loading" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing Payment...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                "unavailable" -> {
                    Text("Not Enough Tickets", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Text("Pay ₹$totalAmount • Book $quantity Ticket${if (quantity > 1) "s" else ""}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun startRazorpayCheckout(
    activity: Activity,
    orderId: String,
    amountInRupees: Int,
    eventTitle: String,
    onPaymentSuccess: (String) -> Unit,
    onPaymentError: (String) -> Unit
) {
    val checkout = Checkout()
    checkout.setKeyID(ApiKeys.RAZORPAY_TEST_KEY_ID)
    try {
        val options = JSONObject().apply {
            put("name", "Event Hive")
            put("description", "Ticket for $eventTitle")
            put("order_id", orderId)
            put("theme.color", "#6200EE")
            put("currency", "INR")
            put("amount", (amountInRupees * 100).toString())
            val prefill = JSONObject()
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                prefill.put("email", it.email ?: "")
                prefill.put("contact", it.phoneNumber ?: "")
            }
            put("prefill", prefill)
        }
        PaymentCallbackHelper.setCallbacks(onPaymentSuccess, onPaymentError)
        checkout.open(activity, options)
    } catch (e: Exception) {
        Log.e(TAG, "Error preparing Razorpay checkout", e)
        Toast.makeText(activity, "Error in payment setup: ${e.message}", Toast.LENGTH_LONG).show()
        onPaymentError(e.message ?: "Unknown error during setup")
    }
}

object PaymentCallbackHelper {
    private var successCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    fun setCallbacks(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        successCallback = onSuccess
        errorCallback = onError
    }

    fun onPaymentSuccess(paymentId: String) {
        successCallback?.invoke(paymentId)
        clearCallbacks()
    }

    fun onPaymentError(error: String) {
        errorCallback?.invoke(error)
        clearCallbacks()
    }

    private fun clearCallbacks() {
        successCallback = null
        errorCallback = null
    }
}
