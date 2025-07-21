package gautam.projects.event_hive.Presntation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import gautam.projects.event_hive.Data.model.SingleEvent
import androidx.compose.ui.layout.ContentScale

@Composable
fun EventCard(
    singleEvent: SingleEvent,
    onCardClick: () -> Unit,
    onJoinClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .clickable { onCardClick() }
            .fillMaxWidth()
            .heightIn(min = 520.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                AsyncImage(
                    model = singleEvent.imageUrls.firstOrNull() ?: "https://via.placeholder.com/300",
                    contentDescription = singleEvent.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "📅 ${singleEvent.date}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = singleEvent.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = singleEvent.locationAddress,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = singleEvent.time,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = singleEvent.description,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Learn More →",
                        fontSize = 15.sp,
                        color = Color.Blue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onCardClick() }
                            .padding(end = 12.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { onJoinClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Join",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Join Now", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
