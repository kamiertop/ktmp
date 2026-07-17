package com.ktmp.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_history",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["played_at"]),
        Index(value = ["media_id"])
    ]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "played_at") val playedAt: Long,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long = 0
)
