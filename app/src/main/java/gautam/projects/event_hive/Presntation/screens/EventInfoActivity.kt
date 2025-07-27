package gautam.projects.event_hive.Presntation.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import gautam.projects.event_hive.ui.theme.EventHiveTheme

class EventInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get eventId from intent
        val eventId = intent.getStringExtra("eventId") ?: ""

        setContent {
            EventHiveTheme {
                if (eventId.isNotEmpty()) {
                    EventInfoScreen(eventId = eventId, activity = this@EventInfoActivity)
                } else {
                    // Fallback to old method if no eventId
                    val title = intent.getStringExtra("title") ?: "No Title"
                    val description = intent.getStringExtra("description") ?: "No Description"
                    val imageUrl = intent.getStringExtra("imageUrl") ?: ""

                    EventInfoContent(
                        title = title,
                        description = description,
                        imageUrl = imageUrl,
                        eventId = eventId,
                        onViewMore = {
                            // Navigate to your main app with the eventId
                            val mainAppIntent = Intent().apply {
                                setClassName(
                                    "gautam.projects.event_hive",
                                    "gautam.projects.event_hive.MainActivity"
                                )
                                putExtra("eventId", eventId)
                                putExtra("navigateTo", "eventInfo")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(mainAppIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EventInfoScreen(eventId: String, activity: ComponentActivity) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(eventId) {
        Firebase.firestore.collection("events").document(eventId)
            .get()
            .addOnSuccessListener { document ->
                isLoading = false
                if (document.exists()) {
                    title = document.getString("title") ?: "No Title"
                    description = document.getString("description") ?: "No Description"
                    imageUrl = document.getString("imageUrl") ?: ""
                } else {
                    error = "Event not found"
                }
            }
            .addOnFailureListener { exception ->
                isLoading = false
                error = "Failed to load event: ${exception.message}"
            }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        error.isNotEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        else -> {
            EventInfoContent(
                title = title,
                description = description,
                imageUrl = imageUrl,
                eventId = eventId,
                onViewMore = {
                    // Navigate to your main app with the eventId
                    val mainAppIntent = Intent().apply {
                        setClassName(
                            "gautam.projects.event_hive",
                            "gautam.projects.event_hive.MainActivity"
                        )
                        putExtra("eventId", eventId)
                        putExtra("navigateTo", "eventInfo")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    activity.startActivity(mainAppIntent)
                    activity.finish()
                }
            )
        }
    }
}

@Composable
fun EventInfoContent(
    title: String,
    description: String,
    imageUrl: String,
    eventId: String = "",
    onViewMore: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUrl.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = "Event Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (description.length > 150) {
                "${description.take(150)}..."
            } else description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Justify
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onViewMore,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "View Full Event Details",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}