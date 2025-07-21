package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import androidx.navigation.NavHostController
import gautam.projects.event_hive.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController
) {
    val recentSearches = remember { mutableStateListOf<String>() }
    var searchText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

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
                onQueryChange = { searchText = it },
                onSearch = {
                    isExpanded = false

                    // First, check if there's any text to process.
                    if (it.isNotBlank()) {
                        // Add to recents only if it's a new term.
                        if (!recentSearches.contains(it)) {
                            recentSearches.add(0, it)
                        }

                        // THEN, clear the text regardless of whether it was new or not.
                        searchText = ""
                    }
                },
                active = isExpanded,
                onActiveChange = { isExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics { isTraversalGroup = true },
                // ✅ CHANGE: Set placeholder text color
                placeholder = { Text("Search By Keyword, Location, Category...", color = Color.Black) },
                // ✅ CHANGE: Set leading icon tint
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
                // ✅ CHANGE: Set divider color to black for theme consistency
                colors = SearchBarDefaults.colors(containerColor = Color.White, dividerColor = Color.Black)
            ) {
                listOf("Music Festivals", "Tech Conferences", "Art Workshops", "Food Fairs").forEach { suggestion ->
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.White),
                        headlineContent = { Text(suggestion, color = Color.Black) },
                        // ✅ CHANGE: Set leading icon tint in suggestions
                        leadingContent = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
                        modifier = Modifier
                            .clickable {
                                searchText = suggestion
                                isExpanded = false
                                if (suggestion.isNotBlank() && !recentSearches.contains(suggestion)) {
                                    recentSearches.add(0, suggestion)
                                }
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
                                searchText = search
                                isExpanded = false
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ CHANGE: Set recent search icon tint to black
                        Icon(imageVector = Icons.Filled.History, contentDescription = "Recent Search", tint = Color.Black)
                        Spacer(Modifier.width(16.dp))
                        Text(text = search, modifier = Modifier.weight(1f), color = Color.Black)
                        IconButton(onClick = { recentSearches.remove(search) }) {
                            // ✅ CHANGE: Set clear icon tint to black
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Remove Search", tint = Color.Black)
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(top = 15.dp)
                    ) {
                        Text("Popular Categories",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black)

                        // You might have this in a LazyRow or Column
                        Row {
                            Card(
                                onClick = { /* Handle card click */ },
                                // Use a shape that matches your design
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(8.dp)
                                    // 1. Size the Card, not the Image directly
                                    .size(width = 180.dp, height = 120.dp)
                            ) {
                                // Box is correct for layering elements
                                Box {
                                    Image(
                                        painter = painterResource(R.drawable.tech),
                                        // 2. ALWAYS provide a meaningful contentDescription for accessibility
                                        contentDescription = "An icon representing the technology category",
                                        // 3. Use ContentScale.Crop to fill the space without distortion
                                        contentScale = ContentScale.Crop,
                                        // This modifier makes the image fill the entire Card
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Text(
                                        text = "Tech",
                                        color = Color.White, // Using white for better contrast on dark images
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            // 4. Add padding to the text so it doesn't touch the edges
                                            .padding(12.dp)
                                    )
                                }
                            }

                            Card(
                                onClick = { /* Handle card click */ },
                                // Use a shape that matches your design
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(8.dp)
                                    // 1. Size the Card, not the Image directly
                                    .size(width = 190.dp, height = 120.dp)
                            ) {
                                // Box is correct for layering elements
                                Box {
                                    Image(
                                        painter = painterResource(R.drawable.art_culture),
                                        // 2. ALWAYS provide a meaningful contentDescription for accessibility
                                        contentDescription = "An icon representing the technology category",
                                        // 3. Use ContentScale.Crop to fill the space without distortion
                                        contentScale = ContentScale.Crop,
                                        // This modifier makes the image fill the entire Card
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Text(
                                        text = "Art & Culture",
                                        color = Color.White, // Using white for better contrast on dark images
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            // 4. Add padding to the text so it doesn't touch the edges
                                            .padding(12.dp)
                                    )
                                }
                            }

                        }
                    }
                    Row {
                        Card(
                            onClick = { /* Handle card click */ },
                            // Use a shape that matches your design
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(8.dp)

                                .size(width = 180.dp, height = 120.dp)
                        ) {

                            Box {
                                Image(
                                    painter = painterResource(R.drawable.music),

                                    contentDescription = "An icon representing the technology category",

                                    contentScale = ContentScale.Crop,

                                    modifier = Modifier.fillMaxSize()
                                )

                                Text(
                                    text = "Music",
                                    color = Color.White, // Using white for better contrast on dark images
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        // 4. Add padding to the text so it doesn't touch the edges
                                        .padding(12.dp)
                                )
                            }
                        }

                        Card(
                            onClick = { /* Handle card click */ },
                            // Use a shape that matches your design
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(8.dp)
                                // 1. Size the Card, not the Image directly
                                .size(width = 190.dp, height = 120.dp)
                        ) {
                            // Box is correct for layering elements
                            Box {
                                Image(
                                    painter = painterResource(R.drawable.food_drink),
                                    // 2. ALWAYS provide a meaningful contentDescription for accessibility
                                    contentDescription = "An icon representing the technology category",
                                    // 3. Use ContentScale.Crop to fill the space without distortion
                                    contentScale = ContentScale.Crop,
                                    // This modifier makes the image fill the entire Card
                                    modifier = Modifier.fillMaxSize()
                                )

                                Text(
                                    text = "Food & Drink",
                                    color = Color.White, // Using white for better contrast on dark images
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        // 4. Add padding to the text so it doesn't touch the edges
                                        .padding(12.dp)
                                )
                            }
                        }

                    }


                }

            }
        }
    }
}