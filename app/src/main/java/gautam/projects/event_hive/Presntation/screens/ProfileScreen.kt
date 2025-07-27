


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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import gautam.projects.event_hive.Data.model.ProfileEvent
import gautam.projects.event_hive.Data.model.User
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel

val placeholderUser = User(
    name = Firebase.auth.currentUser?.displayName,
    email = Firebase.auth.currentUser?.email,
    profilePictureUrl = Firebase.auth.currentUser?.photoUrl
)

val savedEventsPhoto = listOf(
    "https://as2.ftcdn.net/v2/jpg/02/83/73/23/1000_F_283732383_2MNtILcHQlzyTE1LJDzm166yixf5MlKF.jpg",
    "https://as1.ftcdn.net/v2/jpg/01/78/36/16/1000_F_178361629_XUW8h3TErFqqcNxtrZyRR6NNEasZ6iQU.jpg"
)



@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(navController: NavController,
                  viewModel: EventsViewModel,
                  onLogOut: () -> Unit){

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Events", "Saved Events")

    val myEvents = viewModel.myEvents.collectAsState().value.map {
        ProfileEvent(it.imageUrls, it.title, it.date, it.locationAddress, id = it.id)
    }
    val savedEventsList =viewModel.savedEvent.collectAsState().value.map {
        ProfileEvent(it.imageUrls, it.title, it.date, it.locationAddress, id = it.id)
    }

    val eventsToShow = if (selectedTabIndex == 0) myEvents else savedEventsList

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, color = Color.Black) },
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
            item {
                ProfileHeader(
                    user = placeholderUser,
                    onEditClick = {
                        navController.navigate("edit_profile")
                    }
                )
            }

            stickyHeader {
                ProfileTabs(
                    selectedTabIndex = selectedTabIndex,
                    tabs = tabs,
                    onTabSelected = { selectedTabIndex = it }
                )
            }

            if (eventsToShow.isEmpty()) {
                item {
                    Text(
                        text = "No events found.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                items(eventsToShow) { event ->
                    EventListItem(
                        event = event,
                        onClick = {
                            navController.navigate("event_info/${event.id}")
                        }
                    )
                }
            }
        }
    }

    //TODO: the owner of the event cant see the ticket screen so we have to make the admin screen in which we are gonna make them see their respective sales etc


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
        user.name?.let { Text(text = it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,color = MaterialTheme.colorScheme.primary) }
        Spacer(modifier = Modifier.height(4.dp))
        user.email?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge, color = Color.Black) }
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
            AsyncImage(
                model = event.imageUrl.firstOrNull(),
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