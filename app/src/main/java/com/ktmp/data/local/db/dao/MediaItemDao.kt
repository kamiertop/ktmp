package com.ktmp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ktmp.data.local.db.entity.MediaItemEntity
import com.ktmp.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Query("SELECT * FROM media_items ORDER BY date_added DESC")
    fun getAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE media_type = :type ORDER BY title ASC")
    fun getMediaByType(type: MediaType): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT album FROM media_items WHERE album IS NOT NULL AND media_type = :type ORDER BY album ASC")
    fun getAlbums(type: MediaType): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE album = :album ORDER BY track_number ASC, title ASC")
    fun getMediaByAlbum(album: String): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT artist FROM media_items WHERE artist IS NOT NULL AND media_type = :type ORDER BY artist ASC")
    fun getArtists(type: MediaType): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE artist = :artist ORDER BY album ASC, track_number ASC")
    fun getMediaByArtist(artist: String): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT folder_path FROM media_items WHERE folder_path IS NOT NULL AND media_type = :type ORDER BY folder_path ASC")
    fun getFolders(type: MediaType): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE folder_path = :folderPath ORDER BY title ASC")
    fun getMediaByFolder(folderPath: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getMediaByUri(uri: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE uri IN (:uris)")
    suspend fun getMediaByUris(uris: List<String>): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity): Long

    @Delete
    suspend fun delete(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE uri NOT IN (:uris)")
    suspend fun deleteNotIn(uris: List<String>)

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getCount(): Int

    @Query("UPDATE media_items SET title = :title WHERE id = :id")
    suspend fun renameMediaItem(id: Long, title: String)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
