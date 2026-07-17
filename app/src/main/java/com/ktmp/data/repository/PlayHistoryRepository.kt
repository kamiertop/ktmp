package com.ktmp.data.repository

import com.ktmp.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface PlayHistoryRepository {
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<MediaItem>>
    suspend fun recordPlay(mediaId: Long, elapsedMs: Long = 0)
}
