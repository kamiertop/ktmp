package com.ktmp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.MediaType

@Composable
fun MediaListItem(
    item: MediaItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val buttonWidthPx = with(density) { 144.dp.toPx() }

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val offsetDp = with(density) { dragOffsetPx.toDp() }
    val isOpen = dragOffsetPx > 0f

    val hasActions = onDelete != null || onRename != null
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnRename by rememberUpdatedState(onRename)
    val currentOnPlayNext by rememberUpdatedState(onPlayNext)
    val currentOnAddToQueue by rememberUpdatedState(onAddToQueue)
    val currentOnAddToPlaylist by rememberUpdatedState(onAddToPlaylist)

    val hasMenu = onPlayNext != null || onAddToQueue != null ||
            onAddToPlaylist != null || onRename != null || onDelete != null

    var showMenu by remember { mutableStateOf(false) }

    val model: Any? = remember(item.albumArtUri, item.mediaType, item.uri) {
        when {
            item.albumArtUri != null -> Uri.parse(item.albumArtUri!!)
            item.mediaType == MediaType.VIDEO -> Uri.parse(item.uri)
            else -> null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
    ) {
        // Layer 1 (bottom): action buttons at right edge
        if (isOpen) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp)
                        .background(Color(0xFF757575))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                currentOnRename?.invoke()
                                dragOffsetPx = 0f
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Edit, "重命名", tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("重命名", color = Color.White, fontSize = 11.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp)
                        .background(Color(0xFFE53935))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                currentOnDelete?.invoke()
                                dragOffsetPx = 0f
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Delete, "删除", tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("删除", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // Layer 2 (top): foreground, slides left to reveal buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = -offsetDp)
                .background(MaterialTheme.colorScheme.surface)
                .then(
                    if (hasActions) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (isOpen) dragOffsetPx = 0f else currentOnClick()
                                    },
                                    onLongPress = {
                                        if (hasMenu) showMenu = true
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        dragOffsetPx = if (dragOffsetPx > buttonWidthPx / 2)
                                            buttonWidthPx else 0f
                                    },
                                    onHorizontalDrag = { _, amount ->
                                        dragOffsetPx =
                                            (dragOffsetPx - amount).coerceIn(0f, buttonWidthPx)
                                    }
                                )
                            }
                    } else Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { currentOnClick() },
                                onLongPress = {
                                    if (hasMenu) showMenu = true
                                }
                            )
                        }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = item.title,
                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.artist.isNullOrBlank() && item.artist != "<unknown>") {
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (!item.album.isNullOrBlank() && item.album != "<unknown>") {
                        Text(
                            text = " · ${item.album}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Layer 3: long-press context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = 64.dp, y = 0.dp)
        ) {
            if (currentOnPlayNext != null) {
                DropdownMenuItem(
                    text = { Text("下一首播放") },
                    onClick = { showMenu = false; currentOnPlayNext?.invoke() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) }
                )
            }
            if (currentOnAddToQueue != null) {
                DropdownMenuItem(
                    text = { Text("加入队尾") },
                    onClick = { showMenu = false; currentOnAddToQueue?.invoke() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }
                )
            }
            if (currentOnAddToPlaylist != null) {
                DropdownMenuItem(
                    text = { Text("添加到合集") },
                    onClick = { showMenu = false; currentOnAddToPlaylist?.invoke() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                )
            }
            if (currentOnRename != null) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = { showMenu = false; currentOnRename?.invoke() },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
            }
            if (currentOnDelete != null) {
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = { showMenu = false; currentOnDelete?.invoke() },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}
