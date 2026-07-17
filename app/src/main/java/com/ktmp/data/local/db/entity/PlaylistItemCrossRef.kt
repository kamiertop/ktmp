package com.ktmp.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id", "position"], unique = true),
        Index(value = ["media_id"])
    ],
    primaryKeys = ["playlist_id", "media_id"]
)
data class PlaylistItemCrossRef(
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "position") val position: Int
)
