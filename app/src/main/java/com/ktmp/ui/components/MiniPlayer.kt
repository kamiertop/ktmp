package com.ktmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ktmp.domain.model.PlaybackState

@Composable
fun MiniPlayer(
    isVisible: Boolean,
    title: String,
    artist: String?,
    albumArtUri: String?,
    playbackState: PlaybackState,
    positionMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onMiniPlayerClick)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (albumArtUri != null) {
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist != null) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (playbackState == PlaybackState.PLAYING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (playbackState == PlaybackState.PLAYING) "Pause" else "Play"
                    )
                }

                IconButton(onClick = onSkipNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next"
                    )
                }
            }

            if (durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
