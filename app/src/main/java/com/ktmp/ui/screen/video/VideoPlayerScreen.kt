package com.ktmp.ui.screen.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import com.ktmp.domain.model.PlaybackState
import kotlinx.coroutines.delay

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun VideoPlayerScreen(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val metadata by viewModel.currentMetadata.collectAsState()
    val currentIndex by viewModel.currentMediaItemIndex.collectAsState()
    val player = viewModel.playerController.player
    val positionMs by viewModel.playerController.positionMs.collectAsState()
    val durationMs by viewModel.playerController.durationMs.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    var showControls by remember { mutableStateOf(true) }

    // 3 秒无操作自动隐藏控制栏
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { showControls = !showControls }
    ) {
        // Video surface — key forces rebuild on track change to fix frozen frame after background
        if (player != null) {
            key(currentIndex) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).also { pv ->
                            pv.player = player
                            pv.useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 顶部：返回按钮 + 标题
        if (showControls) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Text(
                    text = metadata?.title?.toString() ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // 底部：播放控制栏
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 进度条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(positionMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                        onValueChange = { viewModel.playerController.seekTo(it.toLong()) },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        text = formatTime(durationMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 控制按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.playerController.skipToPrevious() }) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "上一首",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.playerController.seekTo((positionMs - 10000).coerceAtLeast(0))
                    }) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "后退10秒",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = {
                        if (playbackState == PlaybackState.PLAYING) viewModel.playerController.pause()
                        else viewModel.playerController.play()
                    }) {
                        Icon(
                            imageVector = if (playbackState == PlaybackState.PLAYING)
                                Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState == PlaybackState.PLAYING) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.playerController.seekTo((positionMs + 10000).coerceAtMost(durationMs))
                    }) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = "前进10秒",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.playerController.skipToNext() }) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "下一首",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
