package com.ktmp.ui.screen.video

import androidx.lifecycle.ViewModel
import com.ktmp.domain.model.PlaybackState
import com.ktmp.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    val playerController: PlayerController
) : ViewModel() {

    val currentMetadata = playerController.currentMetadata
    val currentMediaItemIndex = playerController.currentMediaItemIndex
    val playbackState: StateFlow<PlaybackState> = playerController.playbackState
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
            started = SharingStarted.Eagerly,
            initialValue = PlaybackState.IDLE
        )
}
