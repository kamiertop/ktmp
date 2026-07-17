package com.ktmp.data.repository

import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaItem>>
    fun getAudioMedia(): Flow<List<MediaItem>>
    fun getVideoMedia(): Flow<List<MediaItem>>
    fun getAlbums(type: MediaType): Flow<List<String>>
    fun getMediaByAlbum(album: String): Flow<List<MediaItem>>
    fun getArtists(type: MediaType): Flow<List<String>>
    fun getMediaByArtist(artist: String): Flow<List<MediaItem>>
    fun getFolders(type: MediaType): Flow<List<String>>
    fun getMediaByFolder(folderPath: String): Flow<List<MediaItem>>
    suspend fun getMediaById(id: Long): MediaItem?
    suspend fun deleteMediaItem(id: Long)
    suspend fun renameMediaItem(id: Long, newTitle: String)
    suspend fun scanAndUpdate(): Int
    suspend fun getCount(): Int
}
