package com.ktmp.data.repository

import com.ktmp.data.local.db.dao.PlayHistoryDao
import com.ktmp.data.local.db.entity.PlayHistoryEntity
import com.ktmp.domain.mapper.toDomain
import com.ktmp.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayHistoryRepositoryImpl @Inject constructor(
    private val playHistoryDao: PlayHistoryDao
) : PlayHistoryRepository {

    override fun getRecentlyPlayed(limit: Int): Flow<List<MediaItem>> =
        playHistoryDao.getRecentlyPlayed(limit).map { it.map { e -> e.toDomain() } }

    override suspend fun recordPlay(mediaId: Long, elapsedMs: Long) {
        playHistoryDao.recordPlay(
            PlayHistoryEntity(
                mediaId = mediaId,
                playedAt = System.currentTimeMillis(),
                elapsedMs = elapsedMs
            )
        )
    }
}
