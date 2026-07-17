package com.ktmp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ktmp.data.local.db.converter.Converters
import com.ktmp.data.local.db.dao.MediaItemDao
import com.ktmp.data.local.db.dao.PlayHistoryDao
import com.ktmp.data.local.db.dao.PlaylistDao
import com.ktmp.data.local.db.entity.MediaItemEntity
import com.ktmp.data.local.db.entity.PlayHistoryEntity
import com.ktmp.data.local.db.entity.PlaylistEntity
import com.ktmp.data.local.db.entity.PlaylistItemCrossRef

@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistItemCrossRef::class,
        PlayHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KtmpDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao
}
