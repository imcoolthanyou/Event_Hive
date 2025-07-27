package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import gautam.projects.event_hive.Presntation.ViewModel.EventsViewModel
import gautam.projects.event_hive.Data.model.SingleEvent
import gautam.projects.event_hive.R
import gautam.projects.event_hive.core.Navigation.Routes
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    eventsViewModel: EventsViewModel
) {
    val viewModel: EventsViewModel = viewModel()
    val recentSearches = remember { mutableStateListOf<String>() }
    var searchText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    // Observe search results and loading state
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                title = { Text(text = "Search", color = Color.Black) }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = searchText,
                onQueryChange = { newQuery ->
                    searchText = newQuery
                    // Trigger real-time search as user types
                    viewModel.searchEventsRealTime(newQuery)
                },
                onSearch = { query ->
                    isExpanded = false
                    if (query.isNotBlank()) {
                        // Add to recent searches if it's new
                        if (!recentSearches.contains(query)) {
                            recentSearches.add(0, query)
                            // Keep only last 10 searches
                            if (recentSearches.size > 10) {
                                recentSearches.removeAt(recentSearches.size - 1)
                            }
                        }
                        // Perform immediate search
                        viewModel.searchEvents(query)
                        searchText = ""

                    }
                },
                active = isExpanded,
                onActiveChange = { isExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics { isTraversalGroup = true },
                placeholder = { Text("Search By Keyword, Location, Category...", color = Color.Black) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
                colors = SearchBarDefaults.colors(containerColor = Color.White, dividerColor = Color.Black)
            ) {
                // Search suggestions
                listOf("Tech Conferences", "Music Festivals", "Art Workshops", "Food Fairs", "Sports Events").forEach { suggestion ->
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.White),
                        headlineContent = { Text(suggestion, color = Color.Black) },
                        leadingContent = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
                        modifier = Modifier
                            .clickable {
                                searchText = suggestion
                                isExpanded = false
                                if (!recentSearches.contains(suggestion)) {
                                    recentSearches.add(0, suggestion)
                                }
                                viewModel.searchEvents(suggestion)
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show search results if there's a query (even if results are empty)
                if (searchQuery.isNotEmpty() || isSearching) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Results for '$searchQuery'" else "Search Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            if (searchResults.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearSearchResults() }) {
                                    Text("Clear", color = Color.Blue)
                                }
                            }
                        }
                    }

                    if (isSearching) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.Blue)
                            }
                        }
                    } else {
                        items(
                            items = searchResults,
                            key = { it.id }
                        ) { event ->
                            SearchResultCard(
                                event = event,
                                onClick = {
                                    navController.navigate(Routes.EventInfoScreen.createRoute(event.id))
                                }
                            )
                        }

                        if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No events found for '$searchQuery'",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Show recent searches and categories when no search results
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Searches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    items(
                        items = recentSearches,
                        key = { it }
                    ) { search ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    viewModel.searchEvents(search)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.History, contentDescription = "Recent Search", tint = Color.Black)
                            Spacer(Modifier.width(16.dp))
                            Text(text = search, modifier = Modifier.weight(1f), color = Color.Black)
                            IconButton(onClick = { recentSearches.remove(search) }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Remove Search", tint = Color.Black)
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(top = 15.dp)
                        ) {
                            Text(
                                "Popular Categories",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            // Category cards
                            Row {
                                CategoryCard(
                                    title = "Tech",
                                    imageRes = R.drawable.tech,
                                    onClick = {
                                        viewModel.searchEvents("Tech")
                                        searchText = "Tech"
                                    }
                                )
                                CategoryCard(
                                    title = "Art & Culture",
                                    imageRes = R.drawable.art_culture,
                                    onClick = {
                                        viewModel.searchEvents("Art Culture")
                                        searchText = "Art Culture"
                                    }
                                )
                            }

                            Row {
                                CategoryCard(
                                    title = "Music",
                                    imageRes = R.drawable.music,
                                    onClick = {
                                        viewModel.searchEvents("Music")
                                        searchText = "Music"
                                    }
                                )
                                CategoryCard(
                                    title = "Food & Drink",
                                    imageRes = R.drawable.food_drink,
                                    onClick = {
                                        viewModel.searchEvents("Food Drink")
                                        searchText = "Food Drink"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    event: SingleEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Event image
            if (event.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = event.imageUrls.first(),
                    contentDescription = "Event image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "No image",
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Event details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.locationAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${event.date} • ${event.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(8.dp)
            .size(width = 180.dp, height = 120.dp)
    ) {
        Box {
            Image(
                painter = painterResource(imageRes),
                contentDescription = "Category: $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}