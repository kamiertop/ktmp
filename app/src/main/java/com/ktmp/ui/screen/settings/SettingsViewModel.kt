package com.ktmp.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktmp.data.datastore.UserPreferences
import com.ktmp.data.repository.MediaRepository
import com.ktmp.domain.model.LoopMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _mediaCount = MutableStateFlow(0)
    val mediaCount: StateFlow<Int> = _mediaCount.asStateFlow()

    init {
        viewModelScope.launch {
            _mediaCount.value = mediaRepository.getCount()
        }
    }

    fun rescanMedia() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                mediaRepository.scanAndUpdate()
                _mediaCount.value = mediaRepository.getCount()
            } finally {
                _isScanning.value = false
            }
        }
    }
}
