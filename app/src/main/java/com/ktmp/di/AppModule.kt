package com.ktmp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ktmp.data.local.db.KtmpDatabase
import com.ktmp.data.local.db.dao.MediaItemDao
import com.ktmp.data.local.db.dao.PlayHistoryDao
import com.ktmp.data.local.db.dao.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KtmpDatabase =
        Room.databaseBuilder(context, KtmpDatabase::class.java, "ktmp.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMediaItemDao(db: KtmpDatabase): MediaItemDao = db.mediaItemDao()

    @Provides
    fun providePlaylistDao(db: KtmpDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun providePlayHistoryDao(db: KtmpDatabase): PlayHistoryDao = db.playHistoryDao()
}
