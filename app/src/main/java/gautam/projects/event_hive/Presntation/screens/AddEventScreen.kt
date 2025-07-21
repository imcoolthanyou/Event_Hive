package gautam.projects.event_hive.Presentation.screens

import android.location.Address
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.core.helper.LocationAutocompleteTextField
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    navController: NavHostController,
    viewModel: EventsViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Form state
    var eventTitle by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var totalTickets by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var locationInputText by remember { mutableStateOf("") }
    var verifiedLocation by remember { mutableStateOf<Address?>(null) }
    var didSubmit by remember { mutableStateOf(false) }

    // Picker visibility
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // DateTime pickers state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= System.currentTimeMillis() - 86400000
        }
    )
    val timePickerState = rememberTimePickerState()

    // Launchers
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedImageUri = uris }

    // Observe ViewModel loading state
    val isLoading by viewModel.isCreating.collectAsState()

    // Navigate when creation completes
    LaunchedEffect(isLoading, didSubmit) {
        if (didSubmit && !isLoading) {
            navController.popBackStack()
        }
    }

    // DatePickerDialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        eventDate = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // TimePickerDialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    eventTime = String.format(
                        Locale.getDefault(), "%02d:%02d",
                        timePickerState.hour, timePickerState.minute
                    )
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = { Text("Event Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Location field
            LocationAutocompleteTextField(
                value = locationInputText,
                onValueChange = {
                    locationInputText = it
                    verifiedLocation = null
                },
                onLocationSelected = { addr ->
                    verifiedLocation = addr
                    locationInputText = addr.getAddressLine(0)
                }
            )
            verifiedLocation?.let {
                Text(
                    "Selected: ${it.getAddressLine(0)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        showDatePicker = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (eventDate.isBlank()) "Select Date" else "Date: $eventDate")
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        showTimePicker = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (eventTime.isBlank()) "Select Time" else "Time: $eventTime")
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = eventDescription,
                onValueChange = { if (it.length <= 500) eventDescription = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            Text(
                "${eventDescription.length}/500",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = totalTickets,
                onValueChange = { if (it.all { ch -> ch.isDigit() }) totalTickets = it },
                label = { Text("Number of Tickets to Sell") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { mediaPickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import Media")
            }

            if (selectedImageUri.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Selected Images:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUri) { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    verifiedLocation?.let { loc ->
                        viewModel.createEvent(
                            selectedImageUri,
                            eventTitle,
                            loc,
                            eventDate,
                            eventTime,
                            eventDescription,
                            totalTickets.toIntOrNull() ?: 0
                        )
                        didSubmit = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading &&
                        eventTitle.isNotBlank() &&
                        verifiedLocation != null &&
                        eventDate.isNotBlank() &&
                        eventTime.isNotBlank() &&
                        totalTickets.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                ) else Text("Create Event", fontSize = 18.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}