package com.ktmp.data.repository

import com.ktmp.data.local.db.dao.MediaItemDao
import com.ktmp.data.local.db.dao.PlaylistCounts
import com.ktmp.data.local.db.dao.PlaylistFirstItemInfo
import com.ktmp.data.local.db.dao.PlaylistDao
import com.ktmp.data.local.db.entity.PlaylistEntity
import com.ktmp.data.local.db.entity.PlaylistItemCrossRef
import com.ktmp.domain.mapper.toDomain
import com.ktmp.domain.mapper.toEntity
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val mediaItemDao: MediaItemDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { it.map { e -> e.toDomain() } }

    override suspend fun getPlaylistById(id: Long): Playlist? =
        playlistDao.getPlaylistById(id)?.toDomain()

    override suspend fun createPlaylist(name: String, description: String): Long {
        playlistDao.getPlaylistByName(name)?.let { return it.id }

        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(
            name = name,
            description = description,
            createdAt = now,
            updatedAt = now
        )
        return playlistDao.insertPlaylist(entity)
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.toEntity(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist.toEntity())
    }

    override fun getPlaylistItems(playlistId: Long): Flow<List<MediaItem>> =
        playlistDao.getPlaylistItems(playlistId).map { it.map { e -> e.toDomain() } }

    override suspend fun addItemToPlaylist(playlistId: Long, mediaId: Long) {
        val position = playlistDao.getNextPosition(playlistId)
        playlistDao.addItem(PlaylistItemCrossRef(playlistId, mediaId, position))
        playlistDao.getPlaylistById(playlistId)?.let { playlist ->
            playlistDao.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override fun getAllPlaylistCounts(): Flow<Map<Long, Int>> =
        playlistDao.getAllPlaylistCounts().map { list ->
            list.associate { it.playlistId to it.count }
        }

    override fun getAllPlaylistFirstItemCovers(): Flow<Map<Long, String?>> =
        playlistDao.getAllPlaylistFirstItemCovers().map { list ->
            list.associate { info ->
                // Same logic as MediaListItem: prefer album art, then video URI, else null
                val cover = info.albumArtUri
                    ?: if (info.mediaType == "VIDEO") info.uri else null
                info.playlistId to cover
            }
        }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName, System.currentTimeMillis())
    }

    override suspend fun deletePlaylistOnly(playlistId: Long) {
        val mediaIds = playlistDao.getMediaIdsInPlaylist(playlistId)
        playlistDao.deleteAllItems(playlistId)
        playlistDao.deletePlaylistById(playlistId)
        // Move items to default playlist
        if (mediaIds.isNotEmpty()) {
            val defaultId = createPlaylist("默认合集", "自动保存的文件")
            mediaIds.forEach { mediaId ->
                val pos = playlistDao.getNextPosition(defaultId)
                playlistDao.addItem(PlaylistItemCrossRef(defaultId, mediaId, pos))
            }
        }
    }

    override suspend fun deletePlaylistAndItems(playlistId: Long) {
        val mediaIds = playlistDao.getMediaIdsInPlaylist(playlistId)
        playlistDao.deleteAllItems(playlistId)
        playlistDao.deletePlaylistById(playlistId)
        if (mediaIds.isNotEmpty()) {
            mediaItemDao.deleteByIds(mediaIds)
        }
    }

    override suspend fun removeItemFromPlaylist(playlistId: Long, mediaId: Long) {
        playlistDao.removeItem(playlistId, mediaId)
    }

    override suspend fun reorderPlaylistItems(playlistId: Long, mediaIdsInOrder: List<Long>) {
        playlistDao.reorderItems(playlistId, mediaIdsInOrder)
    }
}
