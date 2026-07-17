package com.ktmp.playback

import com.ktmp.domain.model.LoopMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor() {

    private var timerJob: Job? = null
    private var onTimerExpired: (() -> Unit)? = null

    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun start(durationMs: Long, scope: CoroutineScope, onExpired: () -> Unit) {
        cancel()
        onTimerExpired = onExpired
        timerJob = scope.launch {
            _isActive.value = true
            _remainingMs.value = durationMs
            val tickInterval = 1000L
            while (true) {
                delay(tickInterval)
                val newValue = (_remainingMs.value ?: 0L) - tickInterval
                if (newValue <= 0) {
                    _remainingMs.value = 0L
                    break
                }
                _remainingMs.value = newValue
            }
            onTimerExpired?.invoke()
            cancel()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingMs.value = null
        _isActive.value = false
        onTimerExpired = null
    }

    val formattedRemaining: String
        get() {
            val remaining = _remainingMs.value ?: return "--:--"
            val totalSeconds = remaining / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
}
