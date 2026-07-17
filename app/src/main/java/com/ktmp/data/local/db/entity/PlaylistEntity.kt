package com.ktmp.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "cover_uri") val coverUri: String? = null,
    @ColumnInfo(name = "category") val category: String = "默认",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
