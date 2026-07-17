package com.ktmp.domain.model

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.MediaMetadata

fun MediaItem.toMedia3Item(): Media3MediaItem {
    val artwork = albumArtUri
        ?: if (mediaType == MediaType.VIDEO) uri else null

    return Media3MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artwork?.let { Uri.parse(it) })
                .setExtras(Bundle().apply {
                    putString("media_type", mediaType.name)
                })
                .build()
        )
        .build()
}
