package com.ktmp.ui.screen.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktmp.data.repository.PlayHistoryRepository
import com.ktmp.domain.model.LoopMode
import com.ktmp.domain.model.PlaybackState
import com.ktmp.playback.PlayerController
import com.ktmp.playback.SleepTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    val playerController: PlayerController,
    val sleepTimerManager: SleepTimerManager,
    private val playHistoryRepository: PlayHistoryRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState.IDLE)

    val currentMetadata = playerController.currentMetadata
    val positionMs = playerController.positionMs
    val durationMs = playerController.durationMs
    val loopMode = playerController.loopMode
    val shuffleEnabled = playerController.shuffleEnabled
    val hasNext = playerController.hasNext
    val hasPrevious = playerController.hasPrevious
    val sleepTimerRemaining = sleepTimerManager.remainingMs
    val sleepTimerActive = sleepTimerManager.isActive

    val playlistItems = playerController.playlistItems
    val currentMediaItemIndex = playerController.currentMediaItemIndex

    init {
        // Periodically poll for position updates (MUST be on Main thread for MediaController)
        viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                playerController.dispatchPositionUpdate()
                delay(250)
            }
        }
    }

    fun play() = playerController.play()
    fun pause() = playerController.pause()
    fun skipToNext() {
        recordCurrentPlay()
        playerController.skipToNext()
    }
    fun skipToPrevious() = playerController.skipToPrevious()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun cycleLoopMode() = playerController.cycleLoopMode()
    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)
    fun skipToQueueIndex(index: Int) = playerController.skipToQueueIndex(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playerController.moveQueueItem(fromIndex, toIndex)
    fun clearQueue() = playerController.clearQueue()
    fun toggleShuffle() = playerController.setLoopMode(
        if (playerController.shuffleEnabled.value) LoopMode.ALL else LoopMode.SHUFFLE
    )

    private fun recordCurrentPlay() {
        viewModelScope.launch {
            val items = playerController.playlistItems.value
            val index = playerController.currentMediaItemIndex.value
            val currentItem = items.getOrNull(index) ?: return@launch
            val mediaId = currentItem.mediaId.toLongOrNull() ?: return@launch
            playHistoryRepository.recordPlay(mediaId)
        }
    }
}
