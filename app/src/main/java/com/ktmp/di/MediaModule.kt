package com.ktmp.di

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.ktmp.data.datastore.UserPreferences
import com.ktmp.data.repository.MediaRepository
import com.ktmp.data.repository.PlayHistoryRepository
import com.ktmp.playback.PlayerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    @Provides
    @Singleton
    fun providePlayerController(
        @ApplicationContext context: Context,
        playHistoryRepository: PlayHistoryRepository,
        mediaRepository: MediaRepository,
        userPreferences: UserPreferences
    ): PlayerController =
        PlayerController(context, playHistoryRepository, mediaRepository, userPreferences)
}
