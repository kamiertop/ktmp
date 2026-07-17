package com.ktmp.ui.screen.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.ktmp.domain.model.LoopMode
import com.ktmp.domain.model.PlaybackState
import com.ktmp.ui.screen.dialogs.PlayQueueSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onSleepTimerClick: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val metadata by viewModel.currentMetadata.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val loopMode by viewModel.loopMode.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()
    val isSleepTimerActive by viewModel.sleepTimerActive.collectAsState()

    val title = metadata?.title?.toString() ?: "未选择曲目"
    val artist = metadata?.artist?.toString() ?: "未知艺术家"
    val albumArtUri = metadata?.artworkUri?.toString()
    val mediaType = metadata?.extras?.getString("media_type")
    val isVideo = mediaType == "VIDEO"
    val player = viewModel.playerController.player

    val playlistItems by viewModel.playlistItems.collectAsState()
    val currentIndex by viewModel.currentMediaItemIndex.collectAsState()
    var showQueueSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正在播放") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showQueueSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放列表")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 视频：显示 PlayerView；音频：显示封面
            // 用 currentIndex 做 key，每次切歌时强制重建 PlayerView
            // 避免后台切歌后切回来画面冻结的问题
            if (isVideo && player != null) {
                key(currentIndex) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).also { pv ->
                                pv.player = player
                                pv.useController = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArtUri != null) {
                        AsyncImage(
                            model = albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatMs(positionMs), style = MaterialTheme.typography.bodySmall)
                Text(text = formatMs(durationMs), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.cycleLoopMode() }) {
                    Icon(
                        imageVector = when (loopMode) {
                            LoopMode.NONE -> Icons.Default.Repeat
                            LoopMode.ONE -> Icons.Default.RepeatOne
                            LoopMode.ALL -> Icons.Default.Repeat
                            LoopMode.SHUFFLE -> Icons.Default.Shuffle
                        },
                        contentDescription = "循环模式",
                        tint = if (loopMode != LoopMode.NONE) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "上一曲", modifier = Modifier.size(40.dp))
                }

                IconButton(onClick = {
                    if (playbackState == PlaybackState.PLAYING) viewModel.pause() else viewModel.play()
                }) {
                    Icon(
                        imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState == PlaybackState.PLAYING) "暂停" else "播放",
                        modifier = Modifier.size(56.dp)
                    )
                }

                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, "下一曲", modifier = Modifier.size(40.dp))
                }

                IconButton(onClick = onSleepTimerClick) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "定时关闭",
                        tint = if (isSleepTimerActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSleepTimerActive && sleepTimerRemaining != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "定时关闭: ${formatMs(sleepTimerRemaining!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // 播放队列底部弹窗
    if (showQueueSheet) {
        PlayQueueSheet(
            items = playlistItems,
            currentIndex = currentIndex,
            onDismiss = { showQueueSheet = false },
            onRemove = { index ->
                viewModel.removeFromQueue(index)
            },
            onPlayIndex = { index ->
                viewModel.skipToQueueIndex(index)
            },
            onClear = {
                viewModel.clearQueue()
            },
            onMove = { from, to ->
                viewModel.moveQueueItem(from, to)
            }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
