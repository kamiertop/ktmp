package com.ktmp.data.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import java.io.File
import androidx.documentfile.provider.DocumentFile
import com.ktmp.data.local.db.dao.MediaItemDao
import com.ktmp.data.local.db.entity.MediaItemEntity
import com.ktmp.domain.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaItemDao: MediaItemDao
) : MediaScanner {

    override fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    override fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    override suspend fun addFiles(uris: List<Uri>): List<Long> {
        // Persist URI permissions so the app can still access these files later
        for (uri in uris) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers may not support persistable permissions
            }
        }

        // Deduplicate: check which URIs already exist in the database
        val uriStrings = uris.map { it.toString() }
        val existing = mediaItemDao.getMediaByUris(uriStrings).associateBy { it.uri }

        // Only extract metadata for new URIs
        val newUris = uris.filter { it.toString() !in existing }
        val newItems = newUris.mapNotNull { uriToEntity(it) }
        val newIds = if (newItems.isNotEmpty()) mediaItemDao.insertAll(newItems) else emptyList()

        // Build result list: map each input URI to its media ID
        var newIdx = 0
        return uris.map { uri ->
            existing[uri.toString()]?.id ?: run {
                val id = if (newIdx < newIds.size) newIds[newIdx] else -1L
                newIdx++
                id
            }
        }.filter { it > 0 }
    }

    override suspend fun scanDirectory(treeUri: Uri): List<Uri> {
        // Persist tree URI permission so child file URIs remain accessible
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { }

        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val mediaUris = mutableListOf<Uri>()
        collectMediaFiles(rootDoc, mediaUris)
        return mediaUris
    }

    private fun collectMediaFiles(dir: DocumentFile, result: MutableList<Uri>) {
        val cr = context.contentResolver
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                collectMediaFiles(file, result)
            } else if (file.isFile) {
                val mime = file.type ?: cr.getType(file.uri) ?: continue
                if (mime.startsWith("audio/") || mime.startsWith("video/")) {
                    result.add(file.uri)
                }
            }
        }
    }

    private fun uriToEntity(uri: Uri): MediaItemEntity? {
        val cr = context.contentResolver
        val mimeType = cr.getType(uri) ?: guessMimeType(uri) ?: return null
        val isVideo = mimeType.startsWith("video/")
        val isAudio = mimeType.startsWith("audio/")
        if (!isVideo && !isAudio) return null

        var displayName = "未知"
        var fileSize = 0L
        cr.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: "未知"
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
            }
        }

        // Try to get metadata from MediaStore if available
        val mediaStoreUri = when {
            isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> return null
        }

        // Query MediaStore for this file by path
        val projection = arrayOf(
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.ARTIST,
            MediaStore.MediaColumns.ALBUM,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.DATE_ADDED
        )

        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val path = uri.path
        var cursor = path?.let {
            cr.query(mediaStoreUri, projection, selection, arrayOf(it), null)
        }

        val mediaEntity = cursor?.use {
            if (it.moveToFirst()) {
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE))
                    ?: displayName
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.ARTIST))
                val album = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.ALBUM))
                val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION))
                val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)) * 1000

                var coverUri: String? = null
                if (isAudio) coverUri = extractEmbeddedCover(uri)

                MediaItemEntity(
                    uri = uri.toString(),
                    filePath = path,
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtUri = coverUri,
                    durationMs = duration,
                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    dateAdded = dateAdded,
                    lastModified = System.currentTimeMillis(),
                    folderPath = path?.substringBeforeLast('/'),
                    trackNumber = null,
                    year = null
                )
            } else null
        }

        // Fallback: use MediaMetadataRetriever to extract metadata from the content URI.
        // Try FileDescriptor first (more reliable for SAF content URIs),
        // then fall back to direct URI-based access.
        return mediaEntity ?: run {
            val retriever = MediaMetadataRetriever()
            var extractedDuration = 0L
            var extractedTitle = displayName
            var extractedArtist: String? = null
            var extractedAlbum: String? = null
            var coverBytes: ByteArray? = null
            try {
                // FileDescriptor approach works more reliably with SAF content URIs.
                // For file:// URIs, open directly via FileInputStream (most reliable).
                if (uri.scheme == "file") {
                    val file = java.io.File(uri.path ?: "")
                    java.io.FileInputStream(file).use { fis ->
                        retriever.setDataSource(fis.fd)
                    }
                } else {
                    val pfd = try {
                        cr.openFileDescriptor(uri, "r")
                    } catch (_: Exception) { null }
                    if (pfd != null) {
                        retriever.setDataSource(pfd.fileDescriptor)
                        pfd.close()
                    } else {
                        retriever.setDataSource(context, uri)
                    }
                }
                extractedDuration = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }?.let { extractedTitle = it }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { extractedArtist = it }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { extractedAlbum = it }
                // Extract embedded cover art for audio files
                if (isAudio) {
                    coverBytes = retriever.embeddedPicture
                }
            } catch (_: Exception) {
                // MediaMetadataRetriever may fail for some content URIs
            } finally {
                retriever.release()
            }

            // Save embedded cover to cache
            var coverUri: String? = null
            if (coverBytes != null) {
                coverUri = saveCoverToCache(uri, coverBytes!!)
            }

            MediaItemEntity(
                uri = uri.toString(),
                filePath = path,
                title = extractedTitle,
                artist = extractedArtist,
                album = extractedAlbum,
                albumArtUri = coverUri,
                durationMs = extractedDuration,
                mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                fileSize = fileSize,
                mimeType = mimeType,
                dateAdded = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis(),
                folderPath = path?.substringBeforeLast('/'),
                trackNumber = null,
                year = null
            )
        }
    }

    /** Guess MIME type from file extension when ContentResolver can't determine it. */
    private fun guessMimeType(uri: Uri): String? {
        val name = uri.lastPathSegment ?: uri.path ?: return null
        return android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(
                name.substringAfterLast('.', "").lowercase()
            )
    }

    /** Extract embedded cover art from an audio file via MediaMetadataRetriever. */
    private fun extractEmbeddedCover(uri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            val pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (_: Exception) { null }
            if (pfd != null) {
                retriever.setDataSource(pfd.fileDescriptor)
                pfd.close()
            } else {
                retriever.setDataSource(context, uri)
            }
            val picture = retriever.embeddedPicture ?: return null
            saveCoverToCache(uri, picture)
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /** Save cover art bytes to cache and return the file URI string. */
    private fun saveCoverToCache(sourceUri: Uri, bytes: ByteArray): String? {
        return try {
            val coverDir = File(context.cacheDir, "covers")
            coverDir.mkdirs()
            val filename = "cover_${sourceUri.toString().hashCode().toString(16)}.jpg"
            val coverFile = File(coverDir, filename)
            coverFile.writeBytes(bytes)
            coverFile.toURI().toString()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun scanAll(): Int {
        val audioItems = scanAudioFiles()
        val videoItems = try {
            scanVideoFiles()
        } catch (_: Exception) {
            emptyList()
        }

        val allItems = audioItems + videoItems

        if (allItems.isEmpty()) return 0

        val insertedIds = mediaItemDao.insertAll(allItems)
        return insertedIds.size
    }

    private fun scanAudioFiles(): List<MediaItemEntity> {
        val items = mutableListOf<MediaItemEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        ) ?: return items

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val trackCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$id"
                val data = it.getString(dataCol)
                val albumId = it.getLong(albumIdCol)
                val albumArtUri = if (albumId > 0) {
                    "content://media/external/audio/albumart/$albumId"
                } else null

                items.add(
                    MediaItemEntity(
                        uri = uri,
                        filePath = data,
                        title = it.getString(titleCol) ?: "Unknown",
                        artist = it.getString(artistCol),
                        album = it.getString(albumCol),
                        albumArtUri = albumArtUri,
                        durationMs = it.getLong(durationCol),
                        mediaType = MediaType.AUDIO,
                        fileSize = it.getLong(sizeCol),
                        mimeType = it.getString(mimeCol),
                        trackNumber = it.getInt(trackCol).takeIf { idx -> idx > 0 },
                        year = it.getInt(yearCol).takeIf { y -> y > 0 },
                        dateAdded = it.getLong(dateAddedCol) * 1000,
                        lastModified = it.getLong(dateModCol) * 1000,
                        folderPath = data?.substringBeforeLast('/')
                    )
                )
            }
        }
        return items
    }

    private fun scanVideoFiles(): List<MediaItemEntity> {
        val items = mutableListOf<MediaItemEntity>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Video.Media.TITLE + " ASC"
        ) ?: return items

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = "${MediaStore.Video.Media.EXTERNAL_CONTENT_URI}/$id"
                val data = it.getString(dataCol)

                items.add(
                    MediaItemEntity(
                        uri = uri,
                        filePath = data,
                        title = it.getString(titleCol) ?: "Unknown",
                        artist = it.getString(artistCol),
                        album = it.getString(albumCol),
                        albumArtUri = null,
                        durationMs = it.getLong(durationCol),
                        mediaType = MediaType.VIDEO,
                        fileSize = it.getLong(sizeCol),
                        mimeType = it.getString(mimeCol),
                        trackNumber = null,
                        year = null,
                        dateAdded = it.getLong(dateAddedCol) * 1000,
                        lastModified = it.getLong(dateModCol) * 1000,
                        folderPath = data?.substringBeforeLast('/')
                    )
                )
            }
        }
        return items
    }
}
