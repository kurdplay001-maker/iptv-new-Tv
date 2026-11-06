package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiveChannelsGradient
import com.example.ui.theme.MoviesGradient
import com.example.ui.theme.SeriesGradient

@Composable
fun MainCategoryCards(
    liveCount: Int,
    moviesCount: Int,
    seriesCount: Int,
    websitesCount: Int = 5,
    language: String,
    isTvMode: Boolean = false,
    onSelectCategory: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else 10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Row 1: Live TV & Movies
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CategoryCardItem(
                titleKu = "کەناڵەکان",
                titleEn = "Live TV",
                subtitle = "$liveCount کەناڵ",
                icon = Icons.Default.LiveTv,
                gradient = LiveChannelsGradient,
                isTvMode = isTvMode,
                onClick = { onSelectCategory(1) },
                testTag = "card_live_tv",
                modifier = Modifier.weight(1f)
            )

            CategoryCardItem(
                titleKu = "فیلمەکان",
                titleEn = "Movies",
                subtitle = "$moviesCount فیلم",
                icon = Icons.Default.Movie,
                gradient = MoviesGradient,
                isTvMode = isTvMode,
                onClick = { onSelectCategory(2) },
                testTag = "card_movies",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Series & Websites
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CategoryCardItem(
                titleKu = "زنجیرەکان",
                titleEn = "Series",
                subtitle = "$seriesCount زنجیرە",
                icon = Icons.Default.Tv,
                gradient = SeriesGradient,
                isTvMode = isTvMode,
                onClick = { onSelectCategory(3) },
                testTag = "card_series",
                modifier = Modifier.weight(1f)
            )

            CategoryCardItem(
                titleKu = "وێب سایتەکان",
                titleEn = "Websites",
                subtitle = "$websitesCount ماڵپەڕ",
                icon = Icons.Default.Language,
                gradient = listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),
                isTvMode = isTvMode,
                onClick = { onSelectCategory(5) },
                testTag = "card_websites",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CategoryCardItem(
    titleKu: String,
    titleEn: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    isTvMode: Boolean = false,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 1.0f, label = "card_scale")

    Surface(
        modifier = modifier
            .height(if (isTvMode) 130.dp else 112.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color(0xFFFFD700) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .testTag(testTag),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient))
                .padding(if (isTvMode) 16.dp else 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Icon & Subtitle Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isTvMode) 42.dp else 36.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = titleEn,
                            tint = Color.White,
                            modifier = Modifier.size(if (isTvMode) 24.dp else 20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = subtitle,
                                color = Color.White,
                                fontSize = if (isTvMode) 12.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(if (isTvMode) 28.dp else 24.dp)
                                .background(Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open",
                                tint = Color.White,
                                modifier = Modifier.size(if (isTvMode) 16.dp else 14.dp)
                            )
                        }
                    }
                }

                // Titles
                Column {
                    Text(
                        text = titleKu,
                        color = Color.White,
                        fontSize = if (isTvMode) 19.sp else 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = titleEn,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = if (isTvMode) 12.sp else 11.sp
                    )
                }
            }
        }
    }
}
