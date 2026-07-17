package com.ktmp.ui.screen.playlistdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ktmp.domain.model.MediaItem
import com.ktmp.playback.PlayerController
import com.ktmp.ui.components.EmptyStateView
import com.ktmp.ui.components.MediaListItem
import com.ktmp.ui.screen.dialogs.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playerController: PlayerController? = null,
    onBack: () -> Unit,
    onMediaClick: (MediaItem, List<MediaItem>) -> Unit,
    onAddToPlaylist: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsState()
    val items by viewModel.playlistItems.collectAsState()

    // 重命名状态
    var renameTargetId by remember { mutableLongStateOf(-1L) }
    var renameTargetTitle by remember { mutableStateOf("") }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "合集") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 播放全部按钮
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { onMediaClick(items.first(), items) }) {
                            Icon(Icons.Default.PlayArrow, "播放全部")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddToPlaylist) {
                Icon(Icons.Default.Add, "添加曲目")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (items.isEmpty()) {
                EmptyStateView(
                    message = "合集中还没有曲目",
                    actionLabel = "添加曲目",
                    onAction = onAddToPlaylist
                )
            } else {
                LazyColumn {
                    items(items, key = { it.id }) { item ->
                        MediaListItem(
                            item = item,
                            onClick = { onMediaClick(item, items) },
                            onDelete = { viewModel.removeItem(playlistId, item.id) },
                            onRename = {
                                renameTargetId = item.id
                                renameTargetTitle = item.title
                            },
                            onPlayNext = playerController?.let { { it.playNext(item) } },
                            onAddToQueue = playerController?.let { { it.addToQueue(item) } }
                        )
                    }
                }
            }
        }
    }

    // 重命名对话框
    if (renameTargetId > 0) {
        RenameDialog(
            currentTitle = renameTargetTitle,
            onDismiss = { renameTargetId = -1L },
            onConfirm = { newTitle ->
                viewModel.renameItem(renameTargetId, newTitle)
                renameTargetId = -1L
            }
        )
    }
}
