// ProfileScreen.kt
package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import gautam.projects.event_hive.Data.model.ProfileEvent
import gautam.projects.event_hive.Data.model.User

// --- Placeholder Data (Replace with Firebase data later) ---
val placeholderUser = User(
    name = "Ethan Carter",
    email = "ethan.carter@email.com",
    profilePictureUrl = "https://as1.ftcdn.net/v2/jpg/13/65/97/94/1000_F_1365979484_QoVvZV7q2D4FD6pMJjspYAzOA96PFvRn.jpg" // Use a real URL for testing
)

val myEventsList = listOf(
    ProfileEvent("https://as1.ftcdn.net/v2/jpg/12/38/09/96/1000_F_1238099609_tCeVeJoK9pVRPwwRM9239OJfhMxJRdxt.jpg", "Indie Rock Night", "Fri, Jul 12", "The Roxy"),
    ProfileEvent("https://as2.ftcdn.net/v2/jpg/05/90/74/27/1000_F_590742789_10VxOHDKZl4oyVWaqyIWgwo7Sd0TV7i0.jpg", "Stand-Up Comedy Show", "Sat, Jul 13", "The Laugh Factory")
)

val savedEventsList = listOf(
    ProfileEvent("https://as2.ftcdn.net/v2/jpg/02/83/73/23/1000_F_283732383_2MNtILcHQlzyTE1LJDzm166yixf5MlKF.jpg", "Summer Music Festival", "Sat, Aug 3, 2:00 PM", "Grand Park, Los Angeles"),
    ProfileEvent("https://as1.ftcdn.net/v2/jpg/01/78/36/16/1000_F_178361629_XUW8h3TErFqqcNxtrZyRR6NNEasZ6iQU.jpg", "Artisan Food Market", "Sun, Aug 4, 11:00 AM", "The Grove, Los Angeles")
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(navController: NavController) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Events", "Saved Events")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold,
                    color = Color.Black) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Profile Header ---
            item {
                ProfileHeader(
                    user = placeholderUser,
                    onEditClick = {
                        // Navigate to the Edit Profile screen
                            navController.navigate("EditScreen")
                    }
                )
            }

            // --- Sticky Tabs ---
            stickyHeader {
                ProfileTabs(
                    selectedTabIndex = selectedTabIndex,
                    tabs = tabs,
                    onTabSelected = { selectedTabIndex = it }
                )
            }

            // --- Conditional Event List ---
            val eventsToShow = if (selectedTabIndex == 0) myEventsList else savedEventsList
            items(eventsToShow) { event ->
                EventListItem(event = event, onClick = { /* TODO: Navigate to event details */ })
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User, onEditClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(model = user.profilePictureUrl),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = user.email, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}

@Composable
fun ProfileTabs(selectedTabIndex: Int, tabs: List<String>, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor =Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(text = title, fontWeight = FontWeight.SemiBold) }
            )
        }
    }
}

@Composable
fun EventListItem(event: ProfileEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = event.imageUrl),
                contentDescription = event.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder bg
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = event.dateTime, style = MaterialTheme.typography.bodyMedium, color = Color.Gray,
                    fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = event.location, style = MaterialTheme.typography.bodyMedium, color = Color.Gray,
                    fontSize = 12.sp)
            }
        }
    }
}