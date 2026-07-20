package com.ktmp.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ktmp.data.datastore.UserPreferences
import com.ktmp.data.repository.MediaRepository
import com.ktmp.data.repository.PlayHistoryRepository
import com.ktmp.domain.model.LoopMode
import com.ktmp.domain.model.PlaybackState
import com.ktmp.domain.model.toMedia3Item
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playHistoryRepository: PlayHistoryRepository,
    private val mediaRepository: MediaRepository,
    private val userPreferences: UserPreferences
) {
    // MediaController is bound to the main looper.  Keep its callbacks and every controller
    // access on that looper; using Dispatchers.IO here crashes Media3 during queue restoration.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaController: MediaController? = null
    @Volatile private var restoringQueue = false
    @Volatile private var lastQueueSaveAtMs = 0L

    private val _currentMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentMetadata: StateFlow<MediaMetadata?> = _currentMetadata.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _loopMode = MutableStateFlow(LoopMode.NONE)
    val loopMode: StateFlow<LoopMode> = _loopMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _currentMediaItemIndex = MutableStateFlow(0)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

    private val _playlistItems = MutableStateFlow<List<QueueItem>>(emptyList())
    val playlistItems: StateFlow<List<QueueItem>> = _playlistItems.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = when (playbackState) {
                Player.STATE_IDLE -> PlaybackState.IDLE
                Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                Player.STATE_READY -> PlaybackState.READY
                Player.STATE_ENDED -> {
                    // 播放列表已播放完毕，回到开头以便用户重新播放
                    scope.launch {
                        mediaController?.apply {
                            if (mediaItemCount > 0) {
                                seekToDefaultPosition(0)
                                pause()
                            }
                        }
                    }
                    PlaybackState.ENDED
                }
                else -> PlaybackState.IDLE
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            _currentMetadata.value = mediaMetadata
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            // _loopMode is managed by setLoopMode(); only update shuffle-derived state here
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            refreshPlaylist()
            persistQueue()
            mediaItem?.mediaId?.toLongOrNull()?.let { id ->
                scope.launch { playHistoryRepository.recordPlay(id) }
            }
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            refreshPlaylist()
            persistQueue()
        }
    }

    fun connect(sessionToken: SessionToken) {
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            val controller = try {
                future.get()
            } catch (_: Exception) {
                return@addListener
            }
            // Future completion is not guaranteed to run on the controller's application looper.
            scope.launch {
                connectOnMainThread(controller)
            }
        }, MoreExecutors.directExecutor())
    }

    private suspend fun connectOnMainThread(controller: MediaController) {
        mediaController = controller
        // Ignore timeline callbacks until persisted state has been considered.
        restoringQueue = true
        controller.addListener(listener)
        _isConnected.value = true
        _loopMode.value = when {
            controller.shuffleModeEnabled -> LoopMode.SHUFFLE
            controller.repeatMode == Player.REPEAT_MODE_ONE -> LoopMode.ONE
            controller.repeatMode == Player.REPEAT_MODE_ALL -> LoopMode.ALL
            else -> LoopMode.NONE
        }
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _currentMediaItemIndex.value = controller.currentMediaItemIndex
        restoreQueue(controller)
        restoringQueue = false
        refreshPlaylist()
    }

    fun play() { mediaController?.play() }
    fun pause() { mediaController?.pause() }
    fun stop() { mediaController?.stop() }

    fun playMedia(items: List<MediaItem>, startIndex: Int = 0) {
        mediaController?.apply {
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
            persistQueue()
        }
    }

    fun skipToNext() { mediaController?.seekToNextMediaItem() }
    fun skipToPrevious() { mediaController?.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { mediaController?.seekTo(positionMs) }

    fun playNext(domainItem: com.ktmp.domain.model.MediaItem) {
        val mc = mediaController ?: return
        val index = mc.currentMediaItemIndex + 1
        mc.addMediaItem(index, domainItem.toMedia3Item())
        refreshPlaylist()
        persistQueue()
    }

    fun addToQueue(domainItem: com.ktmp.domain.model.MediaItem) {
        val mc = mediaController ?: return
        mc.addMediaItem(mc.mediaItemCount, domainItem.toMedia3Item())
        refreshPlaylist()
        persistQueue()
    }

    fun addMultipleToQueue(domainItems: List<com.ktmp.domain.model.MediaItem>) {
        val mc = mediaController ?: return
        if (domainItems.isEmpty()) return
        val endIndex = mc.mediaItemCount
        for ((i, item) in domainItems.withIndex()) {
            mc.addMediaItem(endIndex + i, item.toMedia3Item())
        }
        refreshPlaylist()
        persistQueue()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val mc = mediaController ?: return
        mc.moveMediaItem(fromIndex, toIndex)
        refreshPlaylist()
        persistQueue()
    }

    fun clearQueue() {
        val mc = mediaController ?: return
        val currentIdx = mc.currentMediaItemIndex
        // Remove all items after the current one
        while (mc.mediaItemCount > currentIdx + 1) {
            mc.removeMediaItem(currentIdx + 1)
        }
        refreshPlaylist()
        persistQueue()
    }

    fun setLoopMode(mode: LoopMode) {
        val controller = mediaController ?: return
        _loopMode.value = mode
        when (mode) {
            LoopMode.NONE -> {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_OFF
            }
            LoopMode.ONE -> {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ONE
            }
            LoopMode.ALL -> {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_OFF
            }
            LoopMode.SHUFFLE -> {
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.shuffleModeEnabled = true
            }
        }
    }

    fun cycleLoopMode() {
        val next = LoopMode.entries[(_loopMode.value.ordinal + 1) % LoopMode.entries.size]
        setLoopMode(next)
    }

    fun release() {
        persistQueue()
        mediaController?.removeListener(listener)
        mediaController?.release()
        mediaController = null
        _isConnected.value = false
    }

    fun dispatchPositionUpdate() {
        mediaController?.let {
            _positionMs.value = it.currentPosition
            _durationMs.value = it.duration
            persistQueue(force = false)
        }
    }

    val hasNext: Boolean get() = mediaController?.hasNextMediaItem() ?: false
    val hasPrevious: Boolean get() = mediaController?.hasPreviousMediaItem() ?: false
    val isPlaying: Boolean get() = mediaController?.isPlaying ?: false

    /** Expose the Player so PlayerView can attach to it for video */
    val player: Player? get() = mediaController

    fun refreshPlaylist() {
        val mc = mediaController ?: return
        val items = (0 until mc.mediaItemCount).map { i ->
            val mediaItem = mc.getMediaItemAt(i)
            QueueItem(
                index = i,
                mediaId = mediaItem.mediaId,
                title = mediaItem.mediaMetadata.title?.toString() ?: "未知",
                artist = mediaItem.mediaMetadata.artist?.toString(),
                artworkUri = mediaItem.mediaMetadata.artworkUri?.toString(),
                mediaType = mediaItem.mediaMetadata.extras?.getString("media_type")
            )
        }
        _playlistItems.value = items
        _currentMediaItemIndex.value = mc.currentMediaItemIndex
    }

    fun removeFromQueue(index: Int) {
        val mc = mediaController ?: return
        mc.removeMediaItem(index)
        refreshPlaylist()
        persistQueue()
    }

    fun skipToQueueIndex(index: Int) {
        val mc = mediaController ?: return
        mc.seekToDefaultPosition(index)
    }

    /** Restore the last queue as paused; the user explicitly starts playback after relaunch. */
    private suspend fun restoreQueue(controller: MediaController) {
        val saved = userPreferences.savedPlaybackQueue.first()
        if (saved.mediaIds.isEmpty()) return
        val items = saved.mediaIds.mapNotNull { mediaRepository.getMediaById(it) }
        if (items.isEmpty()) return
        val restoredIndex = saved.currentIndex.coerceIn(0, items.lastIndex)
        if (controller.mediaItemCount == 0) {
            controller.setMediaItems(items.map { it.toMedia3Item() }, restoredIndex, saved.positionMs)
            controller.prepare()
            controller.pause()
        }
    }

    private fun persistQueue(force: Boolean = true) {
        if (restoringQueue) return
        val now = System.currentTimeMillis()
        if (!force && now - lastQueueSaveAtMs < 2_000) return
        lastQueueSaveAtMs = now
        val controller = mediaController ?: return
        val ids = (0 until controller.mediaItemCount).mapNotNull {
            controller.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        scope.launch {
            userPreferences.savePlaybackQueue(ids, controller.currentMediaItemIndex, controller.currentPosition)
        }
    }

    data class QueueItem(
        val index: Int,
        val mediaId: String,
        val title: String,
        val artist: String?,
        val artworkUri: String?,
        val mediaType: String?
    )
}
