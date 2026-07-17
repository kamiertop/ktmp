package com.ktmp.domain.model

data class MediaItem(
    val id: Long = 0,
    val uri: String,
    val filePath: String? = null,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumArtUri: String? = null,
    val durationMs: Long,
    val mediaType: MediaType,
    val fileSize: Long,
    val mimeType: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val dateAdded: Long,
    val lastModified: Long? = null,
    val folderPath: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val displayTitle: String
        get() = title.ifBlank { uri.substringAfterLast('/') }
}
