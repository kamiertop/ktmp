package com.ktmp.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ktmp.domain.model.MediaType

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["media_type"]),
        Index(value = ["date_added"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "file_path") val filePath: String?,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String?,
    @ColumnInfo(name = "album") val album: String?,
    @ColumnInfo(name = "album_art_uri") val albumArtUri: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "media_type") val mediaType: MediaType,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    @ColumnInfo(name = "track_number") val trackNumber: Int?,
    @ColumnInfo(name = "year") val year: Int?,
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long?,
    @ColumnInfo(name = "folder_path") val folderPath: String?
)
