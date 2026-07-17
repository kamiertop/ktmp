package com.ktmp.ui.screen.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktmp.data.repository.MediaRepository
import com.ktmp.data.repository.PlaylistRepository
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist

    private val _playlistItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlistItems: StateFlow<List<MediaItem>> = _playlistItems

    fun loadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _playlist.value = playlistRepository.getPlaylistById(playlistId)
        }
        viewModelScope.launch {
            playlistRepository.getPlaylistItems(playlistId).collect { items ->
                _playlistItems.value = items
            }
        }
    }

    fun removeItem(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            playlistRepository.removeItemFromPlaylist(playlistId, mediaId)
        }
    }

    fun renameItem(mediaId: Long, newTitle: String) {
        viewModelScope.launch {
            mediaRepository.renameMediaItem(mediaId, newTitle)
        }
    }
}
