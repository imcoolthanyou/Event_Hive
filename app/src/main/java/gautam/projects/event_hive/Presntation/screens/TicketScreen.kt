package gautam.projects.event_hive.Presntation.screens

import android.app.Activity
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
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    navController: NavHostController,
    eventId: String?,
    viewModel: EventsViewModel = viewModel()
) {
    val context = LocalContext.current as Activity
    val event by viewModel.selectedEvent.collectAsState()
    val razorpayOrderResponse by viewModel.razorpayOrderResponse.collectAsState()
    var isBooking by remember { mutableStateOf(false) }
    var ticketQuantity by remember { mutableStateOf(1) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    val ticketPrice = 100 // You can make this dynamic based on event

    // Load event details
    LaunchedEffect(eventId) {
        if (eventId != null && event?.id != eventId) {
            viewModel.getEventById(eventId)
        }
    }

    // Handle Razorpay response
    LaunchedEffect(razorpayOrderResponse) {
        razorpayOrderResponse?.let { response ->
            val orderId = response["orderId"] as? String
            if (orderId != null) {
                startRazorpayCheckout(
                    activity = context,
                    orderId = orderId,
                    amountInRupees = ticketPrice * ticketQuantity,
                    eventTitle = event?.title ?: "Event Ticket",
                    onPaymentSuccess = { paymentId ->
                        isBooking = false
                        // Here you might want to call a different method that handles multiple tickets
                        repeat(ticketQuantity) {
                            viewModel.bookTicket(eventId!!)
                        }
                        Toast.makeText(
                            context,
                            "Payment Successful! Booking confirmed for $ticketQuantity ticket(s)",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.popBackStack()
                    },
                    onPaymentError = { error ->
                        isBooking = false
                        Toast.makeText(
                            context,
                            "Payment Failed: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
            viewModel.clearOrderResponse()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Book Tickets",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->

        if (event == null) {
            LoadingScreen()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Event Header Card
                EventHeaderCard(event = event!!)

                Spacer(modifier = Modifier.height(16.dp))

                // Ticket Details Card
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

                // Payment Summary Card
                PaymentSummaryCard(
                    ticketPrice = ticketPrice,
                    quantity = ticketQuantity,
                    totalAmount = ticketPrice * ticketQuantity
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Book Button
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading event details...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventHeaderCard(event: gautam.projects.event_hive.Data.model.SingleEvent) {
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
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                EventDetailRow(
                    icon = Icons.Default.LocationOn,
                    text = event.locationAddress
                )

                Spacer(modifier = Modifier.height(8.dp))

                EventDetailRow(
                    icon = Icons.Default.DateRange,
                    text = "${event.date} at ${event.time}"
                )

                Spacer(modifier = Modifier.height(8.dp))

                EventDetailRow(
                    icon = Icons.Outlined.ConfirmationNumber,
                    text = "${event.ticketsAvailable} tickets available"
                )
            }
        }
    }
}

//@Composable
//private fun EventDetailRow(
//    icon: androidx.compose.ui.graphics.vector.ImageVector,
//    text: String
//) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = null,
//            modifier = Modifier.size(20.dp),
//            tint = MaterialTheme.colorScheme.primary
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        Text(
//            text = text,
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//    }
//}

@Composable
private fun TicketDetailsCard(
    event: gautam.projects.event_hive.Data.model.SingleEvent,
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
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Ticket Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Regular Ticket",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹$ticketPrice per ticket",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quantity Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChange(ticketQuantity - 1) },
                        enabled = ticketQuantity > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Decrease quantity",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = ticketQuantity.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = { onQuantityChange(ticketQuantity + 1) },
                        enabled = ticketQuantity < minOf(5, event.ticketsAvailable),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Increase quantity",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentSummaryCard(
    ticketPrice: Int,
    quantity: Int,
    totalAmount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Payment Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            SummaryRow(
                label = "Ticket Price",
                value = "₹$ticketPrice"
            )

            SummaryRow(
                label = "Quantity",
                value = quantity.toString()
            )

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            SummaryRow(
                label = "Total Amount",
                value = "₹$totalAmount",
                isTotal = true
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
            transitionSpec = {
                fadeIn() with fadeOut()
            }
        ) { state ->
            when (state) {
                "loading" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Processing Payment...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                "unavailable" -> {
                    Text(
                        "Not Enough Tickets Available",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        "Pay ₹$totalAmount • Book $quantity Ticket${if (quantity > 1) "s" else ""}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Enhanced Razorpay checkout function
private fun startRazorpayCheckout(
    activity: Activity,
    orderId: String,
    amountInRupees: Int,
    eventTitle: String,
    onPaymentSuccess: (String) -> Unit,
    onPaymentError: (String) -> Unit
) {
    try {
        val options = JSONObject().apply {
            put("name", "Event Hive")
            put("description", "Ticket for $eventTitle")
            put("order_id", orderId)
            put("theme.color", "#6200EE")
            put("currency", "INR")
            put("amount", (amountInRupees * 100).toString())

            // Enhanced prefill
            val prefill = JSONObject()
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                prefill.put("email", it.email ?: "")
                prefill.put("contact", it.phoneNumber ?: "")
                prefill.put("name", it.displayName ?: "")
            }
            put("prefill", prefill)

            // Notes for tracking
            val notes = JSONObject()
            notes.put("event_title", eventTitle)
            notes.put("user_id", user?.uid ?: "")
            put("notes", notes)
        }

        val checkout = Checkout()
        checkout.setKeyID(ApiKeys.RAZORPAY_TEST_KEY_ID)

        // Store the callbacks in a companion object so the Activity can access them
        PaymentCallbackHelper.setCallbacks(onPaymentSuccess, onPaymentError)

        Checkout.clearUserData(activity)
        checkout.open(activity, options)

    } catch (e: Exception) {
        Toast.makeText(activity, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
        onPaymentError(e.message ?: "Unknown error")
        e.printStackTrace()
    }
}

// Helper object to store payment callbacks
object PaymentCallbackHelper {
    private var successCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    fun setCallbacks(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
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