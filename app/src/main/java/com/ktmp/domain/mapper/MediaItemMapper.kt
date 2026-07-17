package com.ktmp.domain.mapper

import com.ktmp.data.local.db.entity.MediaItemEntity
import com.ktmp.domain.model.MediaItem

fun MediaItemEntity.toDomain(): MediaItem = MediaItem(
    id = id,
    uri = uri,
    filePath = filePath,
    title = title,
    artist = artist,
    album = album,
    albumArtUri = albumArtUri,
    durationMs = durationMs,
    mediaType = mediaType,
    fileSize = fileSize,
    mimeType = mimeType,
    trackNumber = trackNumber,
    year = year,
    dateAdded = dateAdded,
    lastModified = lastModified,
    folderPath = folderPath
)

fun MediaItem.toEntity(): MediaItemEntity = MediaItemEntity(
    id = id,
    uri = uri,
    filePath = filePath,
    title = title,
    artist = artist,
    album = album,
    albumArtUri = albumArtUri,
    durationMs = durationMs,
    mediaType = mediaType,
    fileSize = fileSize,
    mimeType = mimeType,
    trackNumber = trackNumber,
    year = year,
    dateAdded = dateAdded,
    lastModified = lastModified,
    folderPath = folderPath
)
