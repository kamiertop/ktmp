package com.ktmp.playback

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFocusHandler @Inject constructor(
    private val audioManager: AudioManager
) {
    private val _hasAudioFocus = MutableStateFlow(false)
    val hasAudioFocus: StateFlow<Boolean> = _hasAudioFocus.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var onFocusLoss: (() -> Unit)? = null
    private var onFocusGain: (() -> Unit)? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                _hasAudioFocus.value = true
                onFocusGain?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _hasAudioFocus.value = false
                onFocusLoss?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume but keep playing
                _hasAudioFocus.value = true
            }
        }
    }

    fun requestFocus(onLoss: () -> Unit, onGain: () -> Unit): Boolean {
        onFocusLoss = onLoss
        onFocusGain = onGain

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        _hasAudioFocus.value = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return _hasAudioFocus.value
    }

    fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        audioFocusRequest = null
        _hasAudioFocus.value = false
    }
}
