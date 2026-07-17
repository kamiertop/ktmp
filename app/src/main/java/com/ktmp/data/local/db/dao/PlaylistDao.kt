package com.ktmp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ktmp.data.local.db.entity.MediaItemEntity
import com.ktmp.data.local.db.entity.PlaylistEntity
import androidx.room.ColumnInfo
import com.ktmp.data.local.db.entity.PlaylistItemCrossRef
import kotlinx.coroutines.flow.Flow

data class PlaylistCounts(
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "cnt") val count: Int
)

data class PlaylistFirstItemInfo(
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "album_art_uri") val albumArtUri: String?,
    @ColumnInfo(name = "uri") val uri: String?,
    @ColumnInfo(name = "media_type") val mediaType: String?
)

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updated_at DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String, updatedAt: Long)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(crossRef: PlaylistItemCrossRef)

    @Query("""
        SELECT m.* FROM media_items m
        INNER JOIN playlist_items pi ON m.id = pi.media_id
        WHERE pi.playlist_id = :playlistId
        ORDER BY pi.position ASC
    """)
    fun getPlaylistItems(playlistId: Long): Flow<List<MediaItemEntity>>

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId AND media_id = :mediaId")
    suspend fun removeItem(playlistId: Long, mediaId: Long)

    @Query("UPDATE playlist_items SET position = :position WHERE playlist_id = :playlistId AND media_id = :mediaId")
    suspend fun updateItemPosition(playlistId: Long, mediaId: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun getNextPosition(playlistId: Long): Int

    @Transaction
    suspend fun reorderItems(playlistId: Long, mediaIdsInOrder: List<Long>) {
        mediaIdsInOrder.forEachIndexed { index, mediaId ->
            updateItemPosition(playlistId, mediaId, index)
        }
    }

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun deleteAllItems(playlistId: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Query("SELECT media_id FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun getMediaIdsInPlaylist(playlistId: Long): List<Long>

    @Query("SELECT playlist_id, COUNT(*) as cnt FROM playlist_items GROUP BY playlist_id")
    fun getAllPlaylistCounts(): Flow<List<PlaylistCounts>>

    @Query("""
        SELECT pi.playlist_id, m.album_art_uri, m.uri, m.media_type
        FROM playlist_items pi
        INNER JOIN media_items m ON pi.media_id = m.id
        WHERE pi.position = 0
    """)
    fun getAllPlaylistFirstItemCovers(): Flow<List<PlaylistFirstItemInfo>>
}
