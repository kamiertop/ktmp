package com.ktmp.data.repository

import com.ktmp.data.local.db.dao.MediaItemDao
import com.ktmp.data.scanner.MediaScanner
import com.ktmp.domain.mapper.toDomain
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val mediaScanner: MediaScanner
) : MediaRepository {

    override fun getAllMedia(): Flow<List<MediaItem>> =
        mediaItemDao.getAllMedia().map { it.map { entity -> entity.toDomain() } }

    override fun getAudioMedia(): Flow<List<MediaItem>> =
        mediaItemDao.getMediaByType(MediaType.AUDIO).map { it.map { e -> e.toDomain() } }

    override fun getVideoMedia(): Flow<List<MediaItem>> =
        mediaItemDao.getMediaByType(MediaType.VIDEO).map { it.map { e -> e.toDomain() } }

    override fun getAlbums(type: MediaType): Flow<List<String>> =
        mediaItemDao.getAlbums(type)

    override fun getMediaByAlbum(album: String): Flow<List<MediaItem>> =
        mediaItemDao.getMediaByAlbum(album).map { it.map { e -> e.toDomain() } }

    override fun getArtists(type: MediaType): Flow<List<String>> =
        mediaItemDao.getArtists(type)

    override fun getMediaByArtist(artist: String): Flow<List<MediaItem>> =
        mediaItemDao.getMediaByArtist(artist).map { it.map { e -> e.toDomain() } }

    override fun getFolders(type: MediaType): Flow<List<String>> =
        mediaItemDao.getFolders(type)

    override fun getMediaByFolder(folderPath: String): Flow<List<MediaItem>> =
        mediaItemDao.getMediaByFolder(folderPath).map { it.map { e -> e.toDomain() } }

    override suspend fun getMediaById(id: Long): MediaItem? =
        mediaItemDao.getMediaById(id)?.toDomain()

    override suspend fun deleteMediaItem(id: Long) {
        mediaItemDao.getMediaById(id)?.let { mediaItemDao.delete(it) }
    }

    override suspend fun renameMediaItem(id: Long, newTitle: String) {
        mediaItemDao.renameMediaItem(id, newTitle)
    }

    override suspend fun scanAndUpdate(): Int = mediaScanner.scanAll()

    override suspend fun getCount(): Int = mediaItemDao.getCount()
}
