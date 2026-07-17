package com.ktmp.data.repository

import com.ktmp.data.local.db.dao.PlaylistCounts
import com.ktmp.data.local.db.dao.PlaylistFirstItemInfo
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: Long): Playlist?
    suspend fun createPlaylist(name: String, description: String): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlist: Playlist)
    fun getPlaylistItems(playlistId: Long): Flow<List<MediaItem>>
    suspend fun addItemToPlaylist(playlistId: Long, mediaId: Long)
    suspend fun removeItemFromPlaylist(playlistId: Long, mediaId: Long)
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    fun getAllPlaylistCounts(): Flow<Map<Long, Int>>
    fun getAllPlaylistFirstItemCovers(): Flow<Map<Long, String?>>
    suspend fun deletePlaylistOnly(playlistId: Long)
    suspend fun deletePlaylistAndItems(playlistId: Long)
    suspend fun reorderPlaylistItems(playlistId: Long, mediaIdsInOrder: List<Long>)
}
