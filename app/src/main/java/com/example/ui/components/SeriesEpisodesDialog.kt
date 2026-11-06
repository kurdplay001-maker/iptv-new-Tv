package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.PlaylistItem
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlayBorder
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlaySurface
import com.example.ui.theme.KurdPlaySurfaceVariant

data class EpisodeModel(
    val episodeNumber: Int,
    val title: String,
    val season: Int = 1,
    val streamUrl: String,
    val duration: String = "45 Min",
    val isFree: Boolean = true
)

@Composable
fun SeriesEpisodesDialog(
    item: PlaylistItem,
    onDismiss: () -> Unit,
    onSelectEpisode: (PlaylistItem) -> Unit
) {
    // Parse custom episodes or generate default episodes for series
    val episodes = remember(item) {
        if (item.episodesList.isNotBlank()) {
            item.episodesList.split(",").mapIndexedNotNull { index, epStr ->
                val parts = epStr.split("|")
                if (parts.size >= 2) {
                    val epTitle = parts[0].trim()
                    val epUrl = parts[1].trim()
                    val seasonNum = if (epTitle.contains("وەرزی ٢") || epTitle.contains("Season 2")) 2 else 1
                    EpisodeModel(
                        episodeNumber = index + 1,
                        title = epTitle,
                        season = seasonNum,
                        streamUrl = epUrl,
                        duration = "${(40..60).random()} Min",
                        isFree = item.isFree
                    )
                } else null
            }
        } else {
            // Default generated episodes for any series without custom list
            val sampleUrls = listOf(
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback2012.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
            )
            (1..8).map { epIndex ->
                val seasonNum = if (epIndex > 5) 2 else 1
                val epNumInSeason = if (epIndex > 5) epIndex - 5 else epIndex
                EpisodeModel(
                    episodeNumber = epIndex,
                    title = "وەرزی $seasonNum - ئەڵقەی $epNumInSeason",
                    season = seasonNum,
                    streamUrl = sampleUrls[(epIndex - 1) % sampleUrls.size],
                    duration = "${(42..58).random()} Min",
                    isFree = item.isFree
                )
            }
        }
    }

    var selectedSeasonFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Season 1, 2: Season 2

    val filteredEpisodes = remember(selectedSeasonFilter, episodes) {
        when (selectedSeasonFilter) {
            1 -> episodes.filter { it.season == 1 }
            2 -> episodes.filter { it.season == 2 }
            else -> episodes
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, KurdPlayBorder, RoundedCornerShape(20.dp)),
            color = KurdPlayBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner Image with Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    if (item.logoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = item.logoUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(KurdPlaySurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = null,
                                tint = KurdPlayBluePrimary,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    // Top Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        KurdPlayBg
                                    )
                                )
                            )
                    )

                    // Close Button Top End
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("close_episodes_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "داخستن",
                            tint = Color.White
                        )
                    }

                    // Series Title & Info at Bottom Left/Right
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = KurdPlayBluePrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "زنجیرەی تەلەفزیۆنی (TV SERIES)",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.category} • ${episodes.size} ئەڵقەی بەردەستە",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Season Selection Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val seasons = listOf("هەموو ئەڵقەکان", "وەرزی ١", "وەرزی ٢")
                    seasons.forEachIndexed { index, name ->
                        val isSelected = selectedSeasonFilter == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSeasonFilter = index },
                            label = { Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KurdPlayBluePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = KurdPlaySurface,
                                labelColor = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Episodes List Header
                Text(
                    text = "لیستی ئەڵقەکان (Select an Episode to Watch):",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Episodes Lazy Column
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredEpisodes) { episode ->
                        EpisodeCardItem(
                            episode = episode,
                            seriesLogo = item.logoUrl,
                            onPlay = {
                                val episodeItem = item.copy(
                                    title = "${item.title} - ${episode.title}",
                                    streamUrl = episode.streamUrl
                                )
                                onSelectEpisode(episodeItem)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeCardItem(
    episode: EpisodeModel,
    seriesLogo: String,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, KurdPlayBorder, RoundedCornerShape(12.dp))
            .clickable { onPlay() }
            .testTag("episode_item_${episode.episodeNumber}"),
        color = KurdPlaySurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KurdPlaySurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (seriesLogo.isNotEmpty()) {
                    AsyncImage(
                        model = seriesLogo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ماوە: ${episode.duration}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "HD 1080p",
                            color = Color(0xFF4CAF50),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Play Button Action
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("سەیرکردن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
