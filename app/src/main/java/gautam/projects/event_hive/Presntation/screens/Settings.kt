package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel

import gautam.projects.event_hive.Presntation.ViewModel.ProfileViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    onLogOut: () -> Unit
) {
    val eventsViewModel: EventsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val userProfile by profileViewModel.userProfile.collectAsState()

    // State to control the visibility of the delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                // TODO: Call a function on the ProfileViewModel to delete the user account and data
                showDeleteDialog = false
                onLogOut() // Log out after deleting
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Settings", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // FIX: Back button only navigates back
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF2F2F7)
                )
            )
        },
        containerColor = Color(0xFFF2F2F7)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Notifications Section ---
            item {
                SectionHeader("NOTIFICATIONS")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsToggleItem(
                        icon = Icons.Default.Campaign,
                        title = "New Events Nearby",
                        checked = userProfile?.newEventsEnabled ?: true,
                        onCheckedChange = { isEnabled ->
                            profileViewModel.updateNotificationPreference("newEventsEnabled", isEnabled)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.EventAvailable,
                        title = "Event Reminders",
                        checked = userProfile?.eventRemindersEnabled ?: true,
                        onCheckedChange = { isEnabled ->
                            profileViewModel.updateNotificationPreference("eventRemindersEnabled", isEnabled)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    var sliderPosition by remember { mutableStateOf(userProfile?.discoveryRadius?.toFloat() ?: 5f) }
                    LaunchedEffect(userProfile) {
                        userProfile?.let { sliderPosition = it.discoveryRadius.toFloat() }
                    }

                    SettingsSliderItem(
                        icon = Icons.Default.Radar,
                        title = "Discovery Radius",
                        value = sliderPosition,
                        onValueChange = { newPosition -> sliderPosition = newPosition },
                        onValueChangeFinished = {
                            profileViewModel.updateDiscoveryRadius(sliderPosition.roundToInt())
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Account Section ---
            item {
                SectionHeader("ACCOUNT")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Log Out",
                        tint = Color.Red,
                        onClick = {
                            eventsViewModel.logOut()
                            onLogOut()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Account",
                        tint = Color.Red,
                        onClick = { showDeleteDialog = true } // Show confirmation dialog
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- More Section ---
            item {
                SectionHeader("MORE")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItem(
                        icon = Icons.Default.Shield,
                        title = "Privacy Policy",
                        onClick = { /* TODO: Open Privacy Policy URL */ },
                        tint = Color.Black
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Outlined.Description,
                        title = "Terms of Service",
                        onClick = { /* TODO: Open ToS URL */ },
                        tint = Color.Black
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.Star,
                        title = "Rate the App",
                        onClick = { /* TODO: Open Play Store link */ },
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account?") },
        text = { Text("This action is permanent and cannot be undone. All your events and data will be deleted.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Helper Composables (no changes needed below this line) ---

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    tint: Color = Color.Black,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(55.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .height(55.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = Color.Black)
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = Color.Black, fontSize = 18.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = Color.Black)
            Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = Color.Black, fontSize = 18.sp)
            Text(
                text = "${value.roundToInt()} km",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 1f..100f,
            steps = 98
        )
    }
}
