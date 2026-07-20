package com.ktmp.playback

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ktmp.data.repository.MediaRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject lateinit var mediaRepository: MediaRepository
    @Inject lateinit var notificationManager: PlaybackNotificationManager
    @Inject lateinit var sleepTimerManager: SleepTimerManager
    @Inject lateinit var audioFocusHandler: AudioFocusHandler

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var currentCoverBitmap: Bitmap? = null

    /** Pauses playback when any headphone-type device (wired/Bluetooth/USB) is disconnected. */
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val headphoneDisconnected = removedDevices.any { it.isHeadphone }
            if (headphoneDisconnected && player?.playWhenReady == true) {
                player?.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionCallback = MediaSessionCallback()

        mediaSession = MediaSession.Builder(this, player!!).build()

        // Register for audio device removal to detect Bluetooth/USB headphone disconnect
        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)

        // Register notification action receiver
        notificationManager.registerActionReceiver(object : PlaybackNotificationManager.PlaybackActionCallback {
            override fun onPlayPause() {
                val p = player ?: return
                if (p.playWhenReady) p.pause() else p.play()
            }

            override fun onSkipNext() {
                player?.seekToNextMediaItem()
            }

            override fun onSkipPrevious() {
                player?.seekToPreviousMediaItem()
            }

            override fun onStop() {
                player?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager.cancel()
                stopSelf()
            }
        })

        // 切歌、播放/暂停时更新通知
        player?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateNotification()
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateNotification()
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (player == null || player?.playWhenReady != true) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        notificationManager.unregisterActionReceiver()
        audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        mediaSession?.run {
            player?.stop()
            release()
        }
        player?.release()
        sleepTimerManager.cancel()
        super.onDestroy()
    }

    inner class MediaSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            updateNotification()
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            // Keep playing even if UI disconnects
        }
    }

    private fun updateNotification() {
        val player = this.player ?: return
        val metadata = player.mediaMetadata
        val isPlaying = player.playWhenReady
        val notification = notificationManager.buildNotification(
            metadata = metadata,
            isPlaying = isPlaying
        )
        currentCoverBitmap?.let { notification.setLargeIcon(it) }
        startForeground(PlaybackNotificationManager.NOTIFICATION_ID, notification.build())

        // Load cover art async and update notification when ready
        val artworkUri = metadata.artworkUri?.toString()
        notificationManager.loadCoverArt(artworkUri) { bitmap ->
            if (bitmap != null && bitmap != currentCoverBitmap) {
                currentCoverBitmap = bitmap
                val player2 = this@MusicService.player ?: return@loadCoverArt
                val updated = notificationManager.buildNotification(
                    metadata = player2.mediaMetadata,
                    isPlaying = player2.playWhenReady
                ).setLargeIcon(bitmap)
                startForeground(PlaybackNotificationManager.NOTIFICATION_ID, updated.build())
            }
        }
    }
}

/** Returns true for headphone/headset type audio output devices. */
private val AudioDeviceInfo.isHeadphone: Boolean
    get() = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET
