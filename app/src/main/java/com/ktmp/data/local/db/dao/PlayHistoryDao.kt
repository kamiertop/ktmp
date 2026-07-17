package com.ktmp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ktmp.data.local.db.entity.MediaItemEntity
import com.ktmp.data.local.db.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {

    @Query("""
        SELECT DISTINCT m.* FROM media_items m
        INNER JOIN play_history ph ON m.id = ph.media_id
        ORDER BY ph.played_at DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<MediaItemEntity>>

    @Insert
    suspend fun recordPlay(entry: PlayHistoryEntity)
}
