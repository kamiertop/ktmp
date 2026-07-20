package com.ktmp.ui.screen.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.Playlist
import com.ktmp.playback.PlayerController
import com.ktmp.ui.components.EmptyStateView
import com.ktmp.ui.components.MediaListItem
import com.ktmp.ui.screen.dialogs.AddToPlaylistDialog
import com.ktmp.ui.screen.dialogs.DeletePlaylistDialog
import com.ktmp.ui.screen.dialogs.RenameDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    playerController: PlayerController,
    onMediaClick: (MediaItem, List<MediaItem>) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onPlayPlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaySelectedNow: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val allMedia by viewModel.allMedia.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playlistCounts by viewModel.playlistCounts.collectAsState()
    val playlistCovers by viewModel.playlistCovers.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedPlaylistIds by viewModel.selectedPlaylistIds.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("全部", "合集")

    // 搜索
    var searchQuery by remember { mutableStateOf("") }

    // 排序
    var sortMode by remember { mutableIntStateOf(0) }
    val sortOptions = listOf("日期", "名称", "艺人", "时长")
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedAndFiltered by remember(allMedia, searchQuery, sortMode) {
        derivedStateOf {
            var list = allMedia
            // 搜索过滤
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase()
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                    (it.artist?.lowercase()?.contains(q) == true)
                }
            }
            // 排序
            when (sortMode) {
                0 -> list.sortedByDescending { it.dateAdded }
                1 -> list.sortedBy { it.displayTitle.lowercase() }
                2 -> list.sortedBy { it.artist?.lowercase() ?: "zzz" }
                3 -> list.sortedBy { it.durationMs }
                else -> list
            }
        }
    }

    // 重命名状态（曲目 / 合集）
    var renameTargetId by remember { mutableLongStateOf(-1L) }
    var renameTargetTitle by remember { mutableStateOf("") }
    var renamePlaylistId by remember { mutableLongStateOf(-1L) }
    var renamePlaylistName by remember { mutableStateOf("") }

    // 删除合集状态
    var deletePlaylistId by remember { mutableLongStateOf(-1L) }
    var deletePlaylistName by remember { mutableStateOf("") }

    // 添加到合集对话框
    val pendingUris by viewModel.pendingUris.collectAsState()

    // 长按菜单 -> 添加到合集（针对已有曲目）
    var addToPlaylistMediaId by remember { mutableLongStateOf(-1L) }

    // FAB 菜单
    var fabMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.setPendingFiles(uris)
        }
    }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val doc = DocumentFile.fromTreeUri(context, treeUri)
            val dirName = doc?.name ?: treeUri.lastPathSegment ?: "导入目录"
            viewModel.importDirectory(treeUri, dirName)
            Toast.makeText(context, "正在导入「$dirName」...", Toast.LENGTH_SHORT).show()
        }
    }

    // 多选模式下，返回键先退出多选
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.exitMultiSelect()
    }

    Scaffold(
        floatingActionButton = {
            if (isMultiSelectMode && selectedTab == 1) return@Scaffold
            Box {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            1 -> onCreatePlaylist()
                            else -> fabMenuExpanded = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == 1) Icons.Default.Add else Icons.Default.FolderOpen,
                        contentDescription = if (selectedTab == 1) "新建合集" else "添加"
                    )
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("选择文件") },
                        onClick = {
                            fabMenuExpanded = false
                            filePickerLauncher.launch(arrayOf("audio/*", "video/*"))
                        },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("导入目录") },
                        onClick = {
                            fabMenuExpanded = false
                            dirPickerLauncher.launch(null)
                        },
                        leadingIcon = { Icon(Icons.Default.Add, null) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isMultiSelectMode && selectedTab == 1) {
                // 多选操作栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { viewModel.exitMultiSelect() }) {
                        Text("取消")
                    }
                    Text(
                        text = "已选 ${selectedPlaylistIds.size} 个",
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = { viewModel.selectAllPlaylists() }) {
                        Text("全选")
                    }
                }
            } else {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // 搜索栏 + 排序
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索曲目...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, "清除")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, "排序")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                sortOptions.forEachIndexed { idx, name ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = name,
                                                fontWeight = if (idx == sortMode) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = { sortMode = idx; showSortMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    if (allMedia.isEmpty()) {
                        EmptyStateView(
                            message = "媒体库为空",
                            actionLabel = "添加文件",
                            onAction = { filePickerLauncher.launch(arrayOf("audio/*", "video/*")) }
                        )
                    } else if (sortedAndFiltered.isEmpty() && searchQuery.isNotBlank()) {
                        EmptyStateView(message = "没有匹配「$searchQuery」的曲目")
                    } else {
                        LazyColumn {
                            items(sortedAndFiltered, key = { it.id }) { item ->
                                MediaListItem(
                                    item = item,
                                    onClick = { onMediaClick(item, sortedAndFiltered) },
                                    onDelete = { viewModel.deleteMediaItem(item.id) },
                                    onRename = {
                                        renameTargetId = item.id
                                        renameTargetTitle = item.title
                                    },
                                    onPlayNext = { playerController.playNext(item) },
                                    onAddToQueue = { playerController.addToQueue(item) },
                                    onAddToPlaylist = {
                                        addToPlaylistMediaId = item.id
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (playlists.isEmpty()) {
                        EmptyStateView(
                            message = "还没有合集",
                            actionLabel = "新建合集",
                            onAction = onCreatePlaylist
                        )
                    } else {
                        Column {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(playlists) { playlist ->
                                    PlaylistCard(
                                        playlist = playlist,
                                        mediaCount = playlistCounts[playlist.id] ?: 0,
                                        coverUri = playlistCovers[playlist.id] ?: playlist.coverUri,
                                        isSelected = playlist.id in selectedPlaylistIds,
                                        isMultiSelectMode = isMultiSelectMode,
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                viewModel.togglePlaylistSelection(playlist.id)
                                            } else {
                                                onPlaylistClick(playlist.id)
                                            }
                                        },
                                        onPlay = { onPlayPlaylist(playlist.id) },
                                        onRename = if (playlist.name == "默认合集") null else {
                                            {
                                                renamePlaylistId = playlist.id
                                                renamePlaylistName = playlist.name
                                            }
                                        },
                                        onDelete = if (playlist.name == "默认合集") null else {
                                            {
                                                deletePlaylistId = playlist.id
                                                deletePlaylistName = playlist.name
                                            }
                                        },
                                        onEnterMultiSelect = {
                                            viewModel.enterMultiSelect(playlist.id)
                                        },
                                        onAddToQueue = {
                                            viewModel.playPlaylist(playlist.id) { items ->
                                                playerController.addMultipleToQueue(items)
                                            }
                                        },
                                        onPlayNow = { onPlayPlaylist(playlist.id) }
                                    )
                                }
                            }
                            // 多选底部操作栏
                            if (isMultiSelectMode) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.addSelectedToQueue() },
                                        enabled = selectedPlaylistIds.isNotEmpty(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("添加到队列")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.playSelectedNow()
                                            onPlaySelectedNow()
                                        },
                                        enabled = selectedPlaylistIds.isNotEmpty(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("立即播放")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 重命名曲目对话框
    if (renameTargetId > 0) {
        RenameDialog(
            currentTitle = renameTargetTitle,
            onDismiss = { renameTargetId = -1L },
            onConfirm = { newTitle ->
                viewModel.renameMediaItem(renameTargetId, newTitle)
                renameTargetId = -1L
            }
        )
    }

    // 重命名合集对话框
    if (renamePlaylistId > 0) {
        RenameDialog(
            currentTitle = renamePlaylistName,
            onDismiss = { renamePlaylistId = -1L },
            onConfirm = { newName ->
                viewModel.renamePlaylist(renamePlaylistId, newName)
                renamePlaylistId = -1L
            }
        )
    }

    // 删除合集对话框
    if (deletePlaylistId > 0) {
        DeletePlaylistDialog(
            playlistName = deletePlaylistName,
            onDismiss = { deletePlaylistId = -1L },
            onDeleteOnly = {
                viewModel.deletePlaylistOnly(deletePlaylistId)
                deletePlaylistId = -1L
            },
            onDeleteAll = {
                viewModel.deletePlaylistAndItems(deletePlaylistId)
                deletePlaylistId = -1L
            }
        )
    }

    // 添加文件到合集对话框
    if (pendingUris.isNotEmpty()) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { viewModel.clearPendingFiles() },
            onAdd = { playlistId ->
                viewModel.addFilesToPlaylist(pendingUris, playlistId)
                viewModel.clearPendingFiles()
            },
            onCreateNew = {
                onCreatePlaylist()
            }
        )
    }

    // 长按菜单 -> 添加曲目到合集
    if (addToPlaylistMediaId > 0) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { addToPlaylistMediaId = -1L },
            onAdd = { playlistId ->
                viewModel.addToPlaylist(playlistId, addToPlaylistMediaId)
                addToPlaylistMediaId = -1L
            },
            onCreateNew = {
                onCreatePlaylist()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    mediaCount: Int,
    coverUri: String? = null,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEnterMultiSelect: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onPlayNow: (() -> Unit)? = null
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete?.invoke()
                false // 保持卡片原位，由数据库更新来刷新列表
            } else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = onDelete != null,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF5252)
                    else -> Color.Transparent
                },
                label = "swipe_delete_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            if (isMultiSelectMode) return@combinedClickable
                            showContextMenu = true
                        }
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 多选模式下显示 checkbox
                    if (isMultiSelectMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckBox
                            else Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = if (isSelected) "已选中" else "未选中",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp).size(24.dp)
                        )
                    }
                    val coverModel = coverUri?.let { Uri.parse(it) }
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = playlist.name,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playlist.description.isNotEmpty()) {
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$mediaCount 个曲目",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isMultiSelectMode && onRename != null) {
                        IconButton(onClick = onRename) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "重命名",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (!isMultiSelectMode) {
                        IconButton(onClick = onPlay) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "播放",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "分类: ${playlist.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            .format(Date(playlist.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 长按上下文菜单
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = DpOffset(x = 64.dp, y = 0.dp)
            ) {
                onAddToQueue?.let {
                    DropdownMenuItem(
                        text = { Text("添加到队列") },
                        onClick = { showContextMenu = false; it() },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }
                    )
                }
                onPlayNow?.let {
                    DropdownMenuItem(
                        text = { Text("立即播放") },
                        onClick = { showContextMenu = false; it() },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                }
                onEnterMultiSelect?.let {
                    DropdownMenuItem(
                        text = { Text("多选") },
                        onClick = { showContextMenu = false; it() },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) }
                    )
                }
                onRename?.let {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = { showContextMenu = false; it() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                }
                onDelete?.let {
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = { showContextMenu = false; it() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}
