package com.ktmp.domain.mapper

import com.ktmp.data.local.db.entity.PlaylistEntity
import com.ktmp.domain.model.Playlist

fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    coverUri = coverUri,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Playlist.toEntity(updatedAt: Long = this.updatedAt): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    coverUri = coverUri,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)
