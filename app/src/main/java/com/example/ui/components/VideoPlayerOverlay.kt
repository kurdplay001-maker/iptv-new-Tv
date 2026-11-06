package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.PlaylistItem
import com.example.ui.theme.KurdPlayBluePrimary

@Composable
fun VideoPlayerOverlay(
    item: PlaylistItem,
    isPlaying: Boolean,
    isFullScreen: Boolean,
    enablePrerollVideo: Boolean = true,
    prerollVideoUrl: String = "",
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit,
    onPlayNextChannel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf(false) }
    var currentUrl by remember(item.streamUrl) { mutableStateOf(item.streamUrl) }
    var isBuffering by remember { mutableStateOf(true) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    // Preserve playback position across fullscreen / layout changes so video doesn't restart
    var savedPlaybackPosition by remember(item.id) { mutableIntStateOf(0) }

    // Pre-roll Video Intro State (Only if enabled and video URL provided)
    var adCountdown by remember { mutableIntStateOf(3) }
    var showPreRollAd by remember(item.id, enablePrerollVideo, prerollVideoUrl) {
        mutableStateOf(enablePrerollVideo && prerollVideoUrl.isNotBlank())
    }

    LaunchedEffect(item.id, enablePrerollVideo, prerollVideoUrl) {
        if (enablePrerollVideo && prerollVideoUrl.isNotBlank()) {
            showPreRollAd = true
            adCountdown = 3
            while (adCountdown > 0) {
                kotlinx.coroutines.delay(1000L)
                adCountdown--
            }
            showPreRollAd = false
        } else {
            showPreRollAd = false
        }
    }

    // Quality Selection State
    var selectedQuality by remember { mutableStateOf("Auto") }

    // Backup working stream URLs if main stream is offline
    val backupStreams = listOf(
        "https://demo.unified-streaming.com/k8s/features/stable/video/mp4/clear/bbb_sunflower_1080p_30fps_normal.mp4",
        "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
    )

    Column(
        modifier = (if (isFullScreen) Modifier.fillMaxSize() else modifier
            .fillMaxWidth())
            .clip(RoundedCornerShape(if (isFullScreen) 0.dp else 14.dp))
            .background(Color(0xFF0F1626))
            .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(if (isFullScreen) 0.dp else 14.dp))
            .testTag("video_player_container")
    ) {
        // 1) Small App Header Bar at the Very Top (Logo + Name) when in normal mode
        if (!isFullScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090D18))
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_kurdplay_1785925538381),
                        contentDescription = "KURD PLAY TV Logo",
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "KURD ",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "PLAY ",
                            color = Color(0xFFFFC107),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "TV",
                            color = Color(0xFFFF3D00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = item.title,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2) Video Screen Area
        Box(
            modifier = if (isFullScreen) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .background(Color.Black)
            },
            contentAlignment = Alignment.Center
        ) {
            // Video Surface Rendering Engine
            key(currentUrl) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            videoViewInstance = this
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            holder.setFormat(android.graphics.PixelFormat.OPAQUE)
                            setZOrderMediaOverlay(true)

                            val headers = mapOf(
                                "User-Agent" to "VLC/3.0.18 LibVLC/3.0.18 (Android)",
                                "Accept" to "*/*",
                                "Connection" to "keep-alive"
                            )

                            try {
                                setVideoURI(Uri.parse(currentUrl), headers)
                                start()
                            } catch (e: Exception) {
                                try {
                                    setVideoURI(Uri.parse(currentUrl))
                                    start()
                                } catch (e2: Exception) {
                                    playbackError = true
                                }
                            }

                            setOnPreparedListener { mp ->
                                isBuffering = false
                                playbackError = false
                                try {
                                    mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                                mp.isLooping = true

                                // Seek back to saved position if toggling fullscreen or layout change
                                if (savedPlaybackPosition > 0) {
                                    try {
                                        mp.seekTo(savedPlaybackPosition)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                mp.start()
                            }

                            setOnInfoListener { _, what, _ ->
                                if (what == 3 || what == 702) {
                                    isBuffering = false
                                }
                                false
                            }

                            setOnErrorListener { _, _, _ ->
                                isBuffering = false
                                playbackError = true
                                onPlayNextChannel?.invoke()
                                true
                            }
                        }
                    },
                    update = { videoView ->
                        videoViewInstance = videoView
                        runCatching {
                            val isCurrentlyPlaying = try { videoView.isPlaying } catch (e: Exception) { false }
                            val currPos = try { videoView.currentPosition } catch (e: Exception) { 0 }

                            if (isCurrentlyPlaying && currPos > 0) {
                                savedPlaybackPosition = currPos
                            }
                            if (!playbackError && !showPreRollAd) {
                                if (isPlaying && !isCurrentlyPlaying) {
                                    if (savedPlaybackPosition > 0 && currPos == 0) {
                                        try { videoView.seekTo(savedPlaybackPosition) } catch (e: Exception) {}
                                    }
                                    try { videoView.start() } catch (e: Exception) {}
                                } else if (!isPlaying && isCurrentlyPlaying) {
                                    try { videoView.pause() } catch (e: Exception) {}
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Fullscreen Minimal Top Controls (ONLY Exit Fullscreen / Minimize & Close Buttons)
            if (isFullScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(KurdPlayBluePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Fullscreen Buttons: Play/Pause, Minimize (Exit Fullscreen) and Close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Play / Pause Toggle
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Minimize / Exit Fullscreen Button (دوگمەی بچوک کردنەوە)
                        IconButton(
                            onClick = onToggleFullScreen,
                            modifier = Modifier
                                .size(36.dp)
                                .background(KurdPlayBluePrimary, CircleShape)
                                .testTag("player_fullscreen_exit_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Bchuk krdnawa",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Close Button (دوگمەی داخستن)
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Red.copy(alpha = 0.85f), CircleShape)
                                .testTag("player_fullscreen_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dakhstan",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 3-Second Pre-Roll Ad Banner Overlay (Only during ad intro)
            if (showPreRollAd) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = KurdPlayBluePrimary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "📢 ڕیکلامی تایبەت / ADVERTISEMENT",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "پەخشی سەرەکی دەستپێدەکات دوای:",
                            color = Color.White,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(KurdPlayBluePrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$adCountdown",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "KurdPlay - 07501191987",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showPreRollAd = false },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("تێپەڕاندن / Skip ⏭", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Buffering Indicator
            if (isBuffering && !playbackError && !showPreRollAd) {
                CircularProgressIndicator(
                    color = KurdPlayBluePrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Playback Error Screen Overlay
            if (playbackError) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "کێشەی هێڵ یان کاتی ڕاژەکاری ڤیدیۆ بەسەرچووە",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                playbackError = false
                                isBuffering = true
                                if (onPlayNextChannel != null) {
                                    onPlayNextChannel()
                                } else {
                                    val nextBackup = backupStreams.random()
                                    currentUrl = nextBackup
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Channel",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کەناڵی دواتر ⏩", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onClose,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("داخستن", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 3) Compact Middle Controls Panel - Rendered ONLY in normal embedded mode
        if (!isFullScreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1626))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Live Badge + Channel Title and Volume / Favorite / Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = KurdPlayBluePrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val context = LocalContext.current
                        val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager }

                        // Volume Sound Button
                        IconButton(
                            onClick = {
                                try {
                                    audioManager?.adjustStreamVolume(
                                        android.media.AudioManager.STREAM_MUSIC,
                                        android.media.AudioManager.ADJUST_RAISE,
                                        android.media.AudioManager.FLAG_SHOW_UI
                                    )
                                } catch (e: Exception) { e.printStackTrace() }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("player_volume_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Increase Volume",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Favorite Button
                        IconButton(
                            onClick = { onToggleFavorite(item) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("player_fav_btn")
                        ) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (item.isFavorite) Color.Red else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Close Channel Button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("player_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Player",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Row 2: Play/Pause, Rewind, Forward, Quality Pills, Fullscreen Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Play / Pause Button
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(30.dp)
                                .background(KurdPlayBluePrimary, CircleShape)
                                .testTag("player_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Rewind 10s
                        IconButton(
                            onClick = {
                                videoViewInstance?.let { vv ->
                                    val curPos = vv.currentPosition
                                    vv.seekTo((curPos - 10000).coerceAtLeast(0))
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                videoViewInstance?.let { vv ->
                                    val curPos = vv.currentPosition
                                    val dur = vv.duration
                                    vv.seekTo((curPos + 10000).coerceAtMost(if (dur > 0) dur else curPos + 10000))
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Video Quality Selector Pills (480P | 720P | 1080P)
                        Surface(
                            color = Color(0xFF131B2A),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F293D))
                        ) {
                            Row(
                                modifier = Modifier.padding(1.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("480P", "720P", "1080P").forEach { q ->
                                    val isSelectedQuality = selectedQuality.contains(q, ignoreCase = true) || (selectedQuality == "Auto" && q == "720P")
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelectedQuality) KurdPlayBluePrimary else Color.Transparent)
                                            .clickable { selectedQuality = q }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = q,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelectedQuality) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Fullscreen Button
                        IconButton(
                            onClick = onToggleFullScreen,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("player_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
