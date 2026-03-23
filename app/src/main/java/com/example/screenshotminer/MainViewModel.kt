package com.example.screenshotminer

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    private val _targetField = MutableStateFlow("Picture")
    val targetField: StateFlow<String> = _targetField

    private val _miningTimeout = MutableStateFlow(60000L)
    val miningTimeout: StateFlow<Long> = _miningTimeout

    private val _redoTimeout = MutableStateFlow(20000L)
    val redoTimeout: StateFlow<Long> = _redoTimeout

    private val _isDeleteEnabled = MutableStateFlow(false)
    val isDeleteEnabled: StateFlow<Boolean> = _isDeleteEnabled

    init {  // ensures that UI always has the latest settings
        viewModelScope.launch {settingsManager.targetField.collectLatest { _targetField.value = it }}
        viewModelScope.launch {settingsManager.miningTimeout.collectLatest { _miningTimeout.value = it }}
        viewModelScope.launch {settingsManager.redoTimeout.collectLatest { _redoTimeout.value = it }}
        viewModelScope.launch {settingsManager.isDeleteEnabled.collectLatest { _isDeleteEnabled.value = it }}
    }

    /**
     * Basically submits new settings to replace the old ones
     */
    fun updateSettings(newTargetField: String, newMiningTimeout: Long, newRedoTimeout: Long, newIsDeleteEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveSettings(newTargetField, newMiningTimeout, newRedoTimeout, newIsDeleteEnabled)
        }
    }
}
