package com.ktmp.ui.screen.dialogs

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ktmp.playback.PlayerController.QueueItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQueueSheet(
    items: List<QueueItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onRemove: (Int) -> Unit,
    onPlayIndex: (Int) -> Unit,
    onClear: () -> Unit,
    onMove: ((Int, Int) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val playedItems = items.take(currentIndex)
    val currentItem = items.getOrNull(currentIndex)
    val upcomingItems = items.drop(currentIndex + 1)

    var showPlayed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "播放列表 (${items.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                if (upcomingItems.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("清空待播")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (items.isEmpty()) {
                Text(
                    text = "播放列表为空",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
                )
            } else {
                LazyColumn {
                    // --- 正在播放 ---
                    if (currentItem != null) {
                        item(key = "current_header") {
                            Text(
                                text = "正在播放",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item(key = "current_${currentItem.index}") {
                            QueueItemRow(
                                item = currentItem,
                                isCurrent = true,
                                isUpcoming = false,
                                onPlay = { onPlayIndex(currentItem.index) },
                                onRemove = null,
                                onMoveUp = null,
                                onMoveDown = null
                            )
                        }
                    }

                    // --- 接下来播放 ---
                    if (upcomingItems.isNotEmpty()) {
                        item(key = "upcoming_header") {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            Text(
                                text = "接下来播放 (${upcomingItems.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        itemsIndexed(
                            upcomingItems,
                            key = { _, item -> "upcoming_${item.index}" }
                        ) { localIdx, item ->
                            val globalIndex = item.index
                            QueueItemRow(
                                item = item,
                                isCurrent = false,
                                isUpcoming = true,
                                onPlay = { onPlayIndex(globalIndex) },
                                onRemove = { onRemove(globalIndex) },
                                onMoveUp = if (localIdx > 0 && onMove != null) {
                                    { onMove(globalIndex, upcomingItems[localIdx - 1].index) }
                                } else null,
                                onMoveDown = if (localIdx < upcomingItems.lastIndex && onMove != null) {
                                    { onMove(globalIndex, upcomingItems[localIdx + 1].index) }
                                } else null
                            )
                        }
                    }

                    // --- 播放历史 ---
                    if (playedItems.isNotEmpty()) {
                        item(key = "played_header") {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPlayed = !showPlayed }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "播放历史 (${playedItems.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (showPlayed) "收起" else "展开",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (showPlayed) {
                            itemsIndexed(
                                playedItems,
                                key = { _, item -> "played_${item.index}" }
                            ) { _, item ->
                                QueueItemRow(
                                    item = item,
                                    isCurrent = false,
                                    isUpcoming = false,
                                    onPlay = { onPlayIndex(item.index) },
                                    onRemove = null,
                                    onMoveUp = null,
                                    onMoveDown = null,
                                    played = true
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
private fun QueueItemRow(
    item: QueueItem,
    isCurrent: Boolean,
    isUpcoming: Boolean,
    onPlay: () -> Unit,
    onRemove: (() -> Unit)?,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    played: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork or icon
        val artworkModel = item.artworkUri?.let { Uri.parse(it) }
        if (artworkModel != null) {
            AsyncImage(
                model = artworkModel,
                contentDescription = item.title,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp).size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (played) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.artist.isNullOrBlank()) {
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (played) 0.4f else 0.7f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Move up/down buttons for reorder
        if (onMoveUp != null) {
            IconButton(
                onClick = onMoveUp,
                modifier = Modifier.size(28.dp)
            ) {
                Text("▲", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onMoveDown != null) {
            IconButton(
                onClick = onMoveDown,
                modifier = Modifier.size(28.dp)
            ) {
                Text("▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Remove button (upcoming items only)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

