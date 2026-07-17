package com.ktmp.di

import com.ktmp.data.repository.MediaRepository
import com.ktmp.data.repository.MediaRepositoryImpl
import com.ktmp.data.repository.PlayHistoryRepository
import com.ktmp.data.repository.PlayHistoryRepositoryImpl
import com.ktmp.data.repository.PlaylistRepository
import com.ktmp.data.repository.PlaylistRepositoryImpl
import com.ktmp.data.scanner.MediaScanner
import com.ktmp.data.scanner.MediaScannerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindPlayHistoryRepository(impl: PlayHistoryRepositoryImpl): PlayHistoryRepository

    @Binds
    @Singleton
    abstract fun bindMediaScanner(impl: MediaScannerImpl): MediaScanner
}
