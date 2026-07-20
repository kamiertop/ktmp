package com.ktmp.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktmp.data.repository.MediaRepository
import com.ktmp.data.repository.PlayHistoryRepository
import com.ktmp.data.repository.PlaylistRepository
import com.ktmp.data.scanner.MediaScanner
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.MediaType
import android.net.Uri
import com.ktmp.domain.model.Playlist
import com.ktmp.domain.model.toMedia3Item
import com.ktmp.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val mediaScanner: MediaScanner,
    private val playerController: PlayerController
) : ViewModel() {

    init {
        viewModelScope.launch {
            playlistRepository.createPlaylist("默认合集", "自动保存的文件")
        }
    }

    val allMedia: StateFlow<List<MediaItem>> = mediaRepository.getAllMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<String>> = mediaRepository.getAlbums(MediaType.AUDIO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<String>> = mediaRepository.getArtists(MediaType.AUDIO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = mediaRepository.getFolders(MediaType.AUDIO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistCounts: StateFlow<Map<Long, Int>> = playlistRepository.getAllPlaylistCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val playlistCovers: StateFlow<Map<Long, String?>> = playlistRepository.getAllPlaylistFirstItemCovers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recentlyPlayed: StateFlow<List<MediaItem>> = playHistoryRepository.getRecentlyPlayed(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    fun hasPermissions(): Boolean = mediaScanner.hasRequiredPermissions()

    fun getRequiredPermissions(): List<String> = mediaScanner.getRequiredPermissions()

    fun scanMedia() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            try {
                val count = mediaRepository.scanAndUpdate()
            } catch (e: Exception) {
                _scanError.value = e.message ?: "扫描失败"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, description)
        }
    }

    // Pending files that need to be added to a playlist after creation
    private val _pendingUris = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val pendingUris: StateFlow<List<android.net.Uri>> = _pendingUris.asStateFlow()

    fun setPendingFiles(uris: List<android.net.Uri>) {
        _pendingUris.value = uris
    }

    fun clearPendingFiles() {
        _pendingUris.value = emptyList()
    }

    fun createPlaylistAndAddPending(name: String, description: String = "") {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name, description)
            val uris = _pendingUris.value
            if (uris.isNotEmpty()) {
                addFilesToPlaylist(uris, playlistId)
            }
            _pendingUris.value = emptyList()
        }
    }

    fun importDirectory(treeUri: Uri, playlistName: String) {
        viewModelScope.launch {
            val uris = mediaScanner.scanDirectory(treeUri)
            if (uris.isNotEmpty()) {
                val playlistId = playlistRepository.createPlaylist(playlistName, "")
                addFilesToPlaylist(uris, playlistId)
            }
        }
    }

    fun deleteMediaItem(id: Long) {
        viewModelScope.launch {
            mediaRepository.deleteMediaItem(id)
        }
    }

    fun renameMediaItem(id: Long, newTitle: String) {
        viewModelScope.launch {
            mediaRepository.renameMediaItem(id, newTitle)
        }
    }

    fun playPlaylist(playlistId: Long, onReady: (List<MediaItem>) -> Unit) {
        viewModelScope.launch {
            val items = playlistRepository.getPlaylistItems(playlistId).first()
            if (items.isNotEmpty()) {
                onReady(items)
            }
        }
    }

    // -- 多选模式 --

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedPlaylistIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPlaylistIds: StateFlow<Set<Long>> = _selectedPlaylistIds.asStateFlow()

    fun enterMultiSelect(playlistId: Long) {
        _isMultiSelectMode.value = true
        _selectedPlaylistIds.value = setOf(playlistId)
    }

    fun togglePlaylistSelection(playlistId: Long) {
        _selectedPlaylistIds.value = _selectedPlaylistIds.value.toMutableSet().apply {
            if (contains(playlistId)) remove(playlistId) else add(playlistId)
        }
        if (_selectedPlaylistIds.value.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    fun selectAllPlaylists() {
        viewModelScope.launch {
            val all = playlistRepository.getAllPlaylists().first()
            _selectedPlaylistIds.value = all.map { it.id }.toSet()
        }
    }

    fun exitMultiSelect() {
        _isMultiSelectMode.value = false
        _selectedPlaylistIds.value = emptySet()
    }

    fun addSelectedToQueue() {
        viewModelScope.launch {
            val ids = _selectedPlaylistIds.value.toList()
            if (ids.isEmpty()) return@launch
            val items = ids.map { playlistRepository.getPlaylistItems(it).first() }.flatten()
            if (items.isNotEmpty()) {
                playerController.addMultipleToQueue(items)
            }
            exitMultiSelect()
        }
    }

    fun playSelectedNow() {
        viewModelScope.launch {
            val ids = _selectedPlaylistIds.value.toList()
            if (ids.isEmpty()) return@launch
            val items = ids.map { playlistRepository.getPlaylistItems(it).first() }.flatten()
            if (items.isNotEmpty()) {
                playerController.playMedia(items.map { it.toMedia3Item() }, 0)
            }
            exitMultiSelect()
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylistOnly(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylistOnly(playlistId)
        }
    }

    fun deletePlaylistAndItems(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylistAndItems(playlistId)
        }
    }

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            mediaScanner.addFiles(uris)
        }
    }

    fun addFilesToDefaultPlaylist(uris: List<Uri>) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist("默认合集", "自动保存的文件")
            val ids = mediaScanner.addFiles(uris)
            ids.forEach { mediaId ->
                playlistRepository.addItemToPlaylist(playlistId, mediaId)
            }
        }
    }

    fun addFilesToPlaylist(uris: List<Uri>, playlistId: Long) {
        viewModelScope.launch {
            val ids = mediaScanner.addFiles(uris)
            ids.forEach { mediaId ->
                playlistRepository.addItemToPlaylist(playlistId, mediaId)
            }
        }
    }

    fun addToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            playlistRepository.addItemToPlaylist(playlistId, mediaId)
        }
    }

    fun createPlaylistAndAddFiles(name: String, description: String, uris: List<Uri>) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name, description)
            addFilesToPlaylist(uris, playlistId)
        }
    }
}
