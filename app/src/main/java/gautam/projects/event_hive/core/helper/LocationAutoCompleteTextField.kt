// LocationAutocompleteTextField.kt
package gautam.projects.event_hive.core.helper

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun LocationAutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (Address) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var suggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    var isTyping by remember { mutableStateOf(false) }

    // This effect runs when the user's input text changes
    LaunchedEffect(value) {
        isTyping = true
        // Debounce: Wait for 500ms of no typing before fetching suggestions
        delay(500)
        if (value.length > 2) {
            suggestions = fetchSuggestions(context, value)
        } else {
            suggestions = emptyList()
        }
        isTyping = false
    }

    Box(modifier = modifier) {
        Column {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Location") },
                placeholder = { Text("Enter event location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (isTyping) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search Location")
                    }
                }
            )

            // Show the suggestions list when there are suggestions
            AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(suggestions) { address ->
                            Text(
                                text = address.getAddressLine(0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // When a suggestion is clicked, call the callback
                                        onLocationSelected(address)
                                        // Clear the suggestions list
                                        suggestions = emptyList()
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to fetch suggestions using Geocoder
private suspend fun fetchSuggestions(context: Context, query: String): List<Address> {
    val geocoder = Geocoder(context)
    return try {
        withContext(Dispatchers.IO) {
            // getFromLocationName can return null, so we handle it gracefully
            geocoder.getFromLocationName(query, 5) ?: emptyList()
        }
    } catch (e: IOException) {
        // Handle network errors or other issues
        emptyList()
    }
}