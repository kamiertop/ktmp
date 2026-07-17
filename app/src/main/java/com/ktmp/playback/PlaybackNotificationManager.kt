package com.ktmp.playback

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaMetadata
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ktmp.MainActivity
import com.ktmp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader = ImageLoader.Builder(context)
        .crossfade(true)
        .build()
    companion object {
        const val CHANNEL_ID = "playback"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "com.ktmp.action.PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.ktmp.action.SKIP_NEXT"
        const val ACTION_SKIP_PREV = "com.ktmp.action.SKIP_PREV"
        const val ACTION_STOP = "com.ktmp.action.STOP"
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var actionReceiver: BroadcastReceiver? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = androidx.core.app.NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(context.getString(R.string.playback_channel))
            .setShowBadge(false)
            .build()
        notificationManager.createNotificationChannel(channel)
    }

    fun registerActionReceiver(callback: PlaybackActionCallback) {
        unregisterActionReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PLAY_PAUSE -> callback.onPlayPause()
                    ACTION_SKIP_NEXT -> callback.onSkipNext()
                    ACTION_SKIP_PREV -> callback.onSkipPrevious()
                    ACTION_STOP -> callback.onStop()
                }
            }
        }
        actionReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_SKIP_NEXT)
            addAction(ACTION_SKIP_PREV)
            addAction(ACTION_STOP)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun unregisterActionReceiver() {
        actionReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        actionReceiver = null
    }

    fun buildNotification(
        metadata: MediaMetadata,
        isPlaying: Boolean
    ): NotificationCompat.Builder {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action buttons
        val prevIntent = PendingIntent.getBroadcast(
            context, 1, Intent(ACTION_SKIP_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            context, 2, Intent(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getBroadcast(
            context, 3, Intent(ACTION_SKIP_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(metadata.title ?: "未知曲目")
            .setContentText(
                if (!metadata.artist.isNullOrEmpty()) metadata.artist.toString()
                else metadata.albumTitle?.toString() ?: ""
            )
            .setContentIntent(openPendingIntent)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_previous, "上一首", prevIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "暂停" else "播放",
                    playPauseIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_next, "下一首", nextIntent
                ).build()
            )

        return builder
    }

    fun loadCoverArt(artworkUri: String?, onLoaded: (Bitmap?) -> Unit) {
        if (artworkUri == null) {
            onLoaded(null)
            return
        }
        scope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(256, 256)
                    .build()
                val result = imageLoader.execute(request)
                val bitmap = (result as? SuccessResult)?.drawable?.let { drawable ->
                    val w = drawable.intrinsicWidth.coerceAtLeast(1)
                    val h = drawable.intrinsicHeight.coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(canvas)
                    bmp
                }
                withContext(Dispatchers.Main) { onLoaded(bitmap) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onLoaded(null) }
            }
        }
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    interface PlaybackActionCallback {
        fun onPlayPause()
        fun onSkipNext()
        fun onSkipPrevious()
        fun onStop()
    }
}
