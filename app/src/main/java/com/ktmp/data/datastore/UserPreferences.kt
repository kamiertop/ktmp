package com.ktmp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ktmp.domain.model.LoopMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_LOOP_MODE = intPreferencesKey("loop_mode")
        val KEY_SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val KEY_SLEEP_TIMER_MS = longPreferencesKey("sleep_timer_ms")
        val KEY_HAS_COMPLETED_INITIAL_SCAN = booleanPreferencesKey("has_completed_initial_scan")
        val KEY_LAST_PLAYED_MEDIA_ID = longPreferencesKey("last_played_media_id")
        val KEY_LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
        val KEY_PLAYBACK_QUEUE_IDS = stringPreferencesKey("playback_queue_ids")
        val KEY_PLAYBACK_QUEUE_INDEX = intPreferencesKey("playback_queue_index")
    }

    val loopMode: Flow<LoopMode> = context.dataStore.data.map { prefs ->
        val ordinal = prefs[KEY_LOOP_MODE] ?: LoopMode.NONE.ordinal
        LoopMode.entries.getOrElse(ordinal) { LoopMode.NONE }
    }

    val shuffleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHUFFLE_ENABLED] ?: false
    }

    val sleepTimerMs: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SLEEP_TIMER_MS]
    }

    val hasCompletedInitialScan: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAS_COMPLETED_INITIAL_SCAN] ?: false
    }

    val lastPlayedMediaId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_PLAYED_MEDIA_ID]
    }

    val lastPlayedPosition: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_PLAYED_POSITION] ?: 0L
    }

    val savedPlaybackQueue: Flow<SavedPlaybackQueue> = context.dataStore.data.map { prefs ->
        SavedPlaybackQueue(
            mediaIds = prefs[KEY_PLAYBACK_QUEUE_IDS].orEmpty()
                .split(',').mapNotNull { it.toLongOrNull() },
            currentIndex = prefs[KEY_PLAYBACK_QUEUE_INDEX] ?: 0,
            positionMs = prefs[KEY_LAST_PLAYED_POSITION] ?: 0L
        )
    }

    suspend fun setLoopMode(mode: LoopMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOOP_MODE] = mode.ordinal
        }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHUFFLE_ENABLED] = enabled
        }
    }

    suspend fun setSleepTimerMs(ms: Long?) {
        context.dataStore.edit { prefs ->
            if (ms != null) {
                prefs[KEY_SLEEP_TIMER_MS] = ms
            } else {
                prefs.remove(KEY_SLEEP_TIMER_MS)
            }
        }
    }

    suspend fun setHasCompletedInitialScan() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_COMPLETED_INITIAL_SCAN] = true
        }
    }

    suspend fun savePlaybackPosition(mediaId: Long, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_PLAYED_MEDIA_ID] = mediaId
            prefs[KEY_LAST_PLAYED_POSITION] = positionMs
        }
    }

    suspend fun savePlaybackQueue(mediaIds: List<Long>, currentIndex: Int, positionMs: Long) {
        context.dataStore.edit { prefs ->
            if (mediaIds.isEmpty()) {
                prefs.remove(KEY_PLAYBACK_QUEUE_IDS)
                prefs.remove(KEY_PLAYBACK_QUEUE_INDEX)
            } else {
                prefs[KEY_PLAYBACK_QUEUE_IDS] = mediaIds.joinToString(",")
                prefs[KEY_PLAYBACK_QUEUE_INDEX] = currentIndex.coerceIn(0, mediaIds.lastIndex)
            }
            prefs[KEY_LAST_PLAYED_POSITION] = positionMs.coerceAtLeast(0L)
        }
    }
}

data class SavedPlaybackQueue(
    val mediaIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long
)
