package com.ktmp.data.local.db.converter

import androidx.room.TypeConverter
import com.ktmp.domain.model.MediaType

class Converters {
    @TypeConverter
    fun fromMediaType(mediaType: MediaType): String = mediaType.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)
}
